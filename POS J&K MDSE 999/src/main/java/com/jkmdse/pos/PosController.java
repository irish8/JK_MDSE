package com.jkmdse.pos;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
public class PosController {
    private final JdbcTemplate db;
    public PosController(JdbcTemplate db) { this.db = db; }

    private Map<String,Object> user(HttpSession s) { return (Map<String,Object>) s.getAttribute("user"); }
    private String role(HttpSession s) { var u=user(s); return u == null ? "" : String.valueOf(u.get("role")); }
    private boolean admin(HttpSession s) { return "ADMIN".equals(role(s)); }
    private String gate(HttpSession s, boolean needsAdmin) { if(user(s)==null) return "redirect:/login"; if(needsAdmin&&!admin(s)) return "redirect:/sale"; return null; }
    private void base(Model m,HttpSession s) { m.addAttribute("user",user(s)); m.addAttribute("role",role(s)); }

    @GetMapping({"/","/login"}) public String login(HttpSession s) { return user(s)==null?"login":admin(s)?"redirect:/admin":"redirect:/sale"; }
    @PostMapping("/login") public String doLogin(@RequestParam String username,@RequestParam String password,HttpSession s,RedirectAttributes ra) {
        var rows=db.queryForList("SELECT id,username,password_hash,full_name,role FROM users WHERE username=? AND is_active=1",username);
        if(rows.isEmpty() || !PasswordUtil.verify(password,String.valueOf(rows.get(0).get("password_hash")))) { ra.addFlashAttribute("error","Username or password is incorrect."); return "redirect:/login"; }
        var r=rows.get(0); s.setAttribute("user", Map.of("id",r.get("id"),"username",r.get("username"),"name",r.get("full_name"),"role",r.get("role")));
        return "redirect:"+("ADMIN".equals(r.get("role"))?"/admin":"/sale");
    }
    @GetMapping("/logout") public String logout(HttpSession s) { s.invalidate(); return "redirect:/login"; }

    @GetMapping("/admin") public String dashboard(HttpSession s,Model m) { String g=gate(s,true);if(g!=null)return g;base(m,s);
        m.addAttribute("sales",db.queryForObject("SELECT COALESCE(SUM(total),0) FROM sales WHERE DATE(created_at)=CURDATE()",BigDecimal.class));
        m.addAttribute("count",db.queryForObject("SELECT COUNT(*) FROM sales WHERE DATE(created_at)=CURDATE()",Integer.class));
        m.addAttribute("products",db.queryForObject("SELECT COUNT(*) FROM products WHERE is_active=1",Integer.class));
        m.addAttribute("low",db.queryForList("SELECT name,stock_qty FROM products WHERE is_active=1 AND stock_qty<=5 ORDER BY stock_qty LIMIT 6"));
        return "admin"; }

    @GetMapping("/products") public String products(@RequestParam(defaultValue="") String q,HttpSession s,Model m){String g=gate(s,true);if(g!=null)return g;base(m,s);m.addAttribute("q",q);m.addAttribute("products",db.queryForList("SELECT * FROM products WHERE is_active=1 AND (name LIKE ? OR category LIKE ?) ORDER BY name", "%"+q+"%","%"+q+"%"));return "products";}
    @PostMapping("/products/save") public String saveProduct(@RequestParam(required=false) Long id,@RequestParam String name,@RequestParam BigDecimal price,@RequestParam String category,@RequestParam int stock_qty,HttpSession s,RedirectAttributes ra){if(!admin(s))return "redirect:/sale";
        if(id==null)db.update("INSERT INTO products(name,price,category,stock_qty) VALUES(?,?,?,?)",name,price,category,stock_qty);else db.update("UPDATE products SET name=?,price=?,category=?,stock_qty=? WHERE id=?",name,price,category,stock_qty,id);ra.addFlashAttribute("notice","Product saved.");return "redirect:/products";}
    @PostMapping("/products/delete/{id}") public String deleteProduct(@PathVariable long id,HttpSession s){if(!admin(s))return "redirect:/sale";db.update("UPDATE products SET is_active=0 WHERE id=?",id);return "redirect:/products";}

    @GetMapping("/users") public String users(HttpSession s,Model m){String g=gate(s,true);if(g!=null)return g;base(m,s);m.addAttribute("users",db.queryForList("SELECT id,username,full_name,role,is_active FROM users ORDER BY full_name"));return "users";}
    @PostMapping("/users/save") public String saveUser(@RequestParam(required=false) Long id,@RequestParam String username,@RequestParam String full_name,@RequestParam String role,@RequestParam(required=false,defaultValue="") String password,HttpSession s,RedirectAttributes ra){if(!admin(s))return "redirect:/sale";
        if(id==null){if(password.length()<8){ra.addFlashAttribute("error","Password must contain at least 8 characters.");return "redirect:/users";}db.update("INSERT INTO users(username,password_hash,full_name,role) VALUES(?,?,?,?)",username,PasswordUtil.hash(password),full_name,role);}
        else {db.update("UPDATE users SET username=?,full_name=?,role=? WHERE id=?",username,full_name,role,id);if(!password.isBlank())db.update("UPDATE users SET password_hash=? WHERE id=?",PasswordUtil.hash(password),id);}return "redirect:/users";}
    @PostMapping("/users/toggle/{id}") public String toggleUser(@PathVariable long id,HttpSession s){if(!admin(s))return "redirect:/sale";if(Objects.equals(user(s).get("id"),id))return "redirect:/users";db.update("UPDATE users SET is_active=1-is_active WHERE id=?",id);return "redirect:/users";}

    @GetMapping("/sale") public String sale(HttpSession s,Model m){String g=gate(s,false);if(g!=null)return g;base(m,s);m.addAttribute("products",db.queryForList("SELECT id,name,price,category,stock_qty FROM products WHERE is_active=1 AND stock_qty>0 ORDER BY name"));return "sale";}
    @PostMapping("/checkout") @Transactional public String checkout(@RequestParam String cart,@RequestParam(defaultValue="NONE") String discount_type,@RequestParam(defaultValue="0") BigDecimal discount_value,@RequestParam BigDecimal paid,HttpSession s,RedirectAttributes ra){String g=gate(s,false);if(g!=null)return g;
        try { Map<?,?> items=(Map<?,?>)new com.fasterxml.jackson.databind.ObjectMapper().readValue(cart,Map.class);if(items.isEmpty())throw new IllegalArgumentException("Cart is empty");
            BigDecimal subtotal=BigDecimal.ZERO;List<Map<String,Object>> lines=new ArrayList<>();for(var e:items.entrySet()){long id=Long.parseLong(e.getKey().toString());int qty=Integer.parseInt(e.getValue().toString());if(qty<1)throw new IllegalArgumentException("Invalid quantity");var p=db.queryForMap("SELECT id,name,price,stock_qty FROM products WHERE id=? AND is_active=1 FOR UPDATE",id);if(qty>((Number)p.get("stock_qty")).intValue())throw new IllegalArgumentException("Insufficient stock for "+p.get("name"));BigDecimal amount=((BigDecimal)p.get("price")).multiply(BigDecimal.valueOf(qty));subtotal=subtotal.add(amount);lines.add(Map.of("id",id,"name",p.get("name"),"price",p.get("price"),"qty",qty,"amount",amount));}
            BigDecimal discount=discount_type.equals("PERCENT")?subtotal.multiply(discount_value).divide(BigDecimal.valueOf(100),2,java.math.RoundingMode.HALF_UP):discount_type.equals("FIXED")?discount_value:BigDecimal.ZERO;if(discount.signum()<0||discount.compareTo(subtotal)>0)throw new IllegalArgumentException("Discount must be between 0 and subtotal");BigDecimal total=subtotal.subtract(discount);if(paid.compareTo(total)<0)throw new IllegalArgumentException("Payment does not cover total.");
            long uid=((Number)user(s).get("id")).longValue();db.update("INSERT INTO sales(transaction_no,user_id,subtotal,discount,total,amount_paid,change_due) VALUES(?,?,?,?,?,?,?)", "JK"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(),uid,subtotal,discount,total,paid,paid.subtract(total));long saleId=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
            for(var line:lines){db.update("INSERT INTO sale_items(sale_id,product_id,product_name,quantity,unit_price,line_total) VALUES(?,?,?,?,?,?)",saleId,line.get("id"),line.get("name"),line.get("qty"),line.get("price"),line.get("amount"));db.update("UPDATE products SET stock_qty=stock_qty-? WHERE id=?",line.get("qty"),line.get("id"));}
            ra.addAttribute("id",saleId);return "redirect:/receipt";
        }catch(Exception ex){TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();ra.addFlashAttribute("error",ex.getMessage()==null?"Could not complete transaction.":ex.getMessage());return "redirect:/sale";}}
    @GetMapping("/receipt") public String receipt(@RequestParam long id,HttpSession s,Model m){String g=gate(s,false);if(g!=null)return g;var rows=db.queryForList("SELECT s.*,u.full_name cashier FROM sales s JOIN users u ON u.id=s.user_id WHERE s.id=?",id);if(rows.isEmpty()||(!admin(s)&&!Objects.equals(((Number)user(s).get("id")).longValue(),((Number)rows.get(0).get("user_id")).longValue())))return "redirect:/history";base(m,s);m.addAttribute("sale",rows.get(0));m.addAttribute("items",db.queryForList("SELECT * FROM sale_items WHERE sale_id=?",id));return "receipt";}
    @GetMapping("/history") public String history(@RequestParam(required=false) String from,@RequestParam(required=false) String to,@RequestParam(required=false) Long cashier,HttpSession s,Model m){String g=gate(s,false);if(g!=null)return g;base(m,s);boolean a=admin(s);String sql="SELECT s.id,s.transaction_no,s.created_at,u.full_name cashier,s.total FROM sales s JOIN users u ON u.id=s.user_id WHERE (?=1 OR s.user_id=?) AND (? IS NULL OR DATE(s.created_at)>=?) AND (? IS NULL OR DATE(s.created_at)<=?) AND (? IS NULL OR s.user_id=?) ORDER BY s.created_at DESC LIMIT 300";
        Long own=((Number)user(s).get("id")).longValue();m.addAttribute("sales",db.queryForList(sql,a?1:0,own,from,from,to,to,cashier,cashier));m.addAttribute("from",from);m.addAttribute("to",to);m.addAttribute("cashier",cashier);m.addAttribute("cashiers",a?db.queryForList("SELECT id,full_name FROM users ORDER BY full_name"):List.of());return "history";}
    @GetMapping("/reports") public String reports(@RequestParam(defaultValue="month") String period,HttpSession s,Model m){String g=gate(s,true);if(g!=null)return g;base(m,s);String grouping=switch(period){case "day"->"DATE(created_at)";case "week"->"YEARWEEK(created_at,1)";case "year"->"YEAR(created_at)";default->"DATE_FORMAT(created_at,'%Y-%m')";};m.addAttribute("period",period);m.addAttribute("report",db.queryForList("SELECT "+grouping+" period,COUNT(*) transactions,COALESCE(SUM(total),0) sales FROM sales GROUP BY period ORDER BY period DESC LIMIT 24"));return "reports";}
}
