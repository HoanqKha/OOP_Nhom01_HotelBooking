package vn.edu.ptit.oop.nhom08.model;
import vn.edu.ptit.oop.nhom08.common.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import vn.edu.ptit.oop.nhom08.DBConnection;

public class UserService {
    private User currentUser;
    public UserService() {}
    public boolean register(String username, String password, String fullName, String phone, String cccd) throws Exception {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new Exception("Tên đăng nhập và mật khẩu không được để trống!");
        }
        // 1. Kiểm tra xem tên đăng nhập đã tồn tại trong CSDL chưa
        String checkSql = "SELECT username FROM NguoiDung WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmtCheck = conn.prepareStatement(checkSql)) {
            pstmtCheck.setString(1, username.trim());
            ResultSet rs = pstmtCheck.executeQuery();
            if (rs.next()) {
                throw new Exception("Tên đăng nhập '" + username + "' đã tồn tại!");
            }
        } catch (SQLException e) {
            throw new Exception("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
        }
        // 2. Thêm tài khoản mới vào CSDL (Mặc định khi đăng ký là CUSTOMER)
        String insertSql = "INSERT INTO NguoiDung (username, password, full_name, role, phone, cccd) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
            pstmtInsert.setString(1, username.trim());
            pstmtInsert.setString(2, password);
            pstmtInsert.setString(3, fullName);
            pstmtInsert.setString(4, Role.CUSTOMER.name()); // Lưu tên Enum dạng chuỗi (CUSTOMER)
            pstmtInsert.setString(5, phone);
            pstmtInsert.setString(6, cccd);
            
            int rowsAffected = pstmtInsert.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new Exception("Lỗi khi đăng ký tài khoản: " + e.getMessage());
        }
    }
    public User login(String username, String password, Role expectedRole) throws Exception {
        if (username == null || password == null) {
            throw new Exception("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
        }
        String sql = "SELECT username, password, full_name, role, phone, cccd FROM NguoiDung WHERE username = ?";
        User u = null;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                // Kiểm tra mật khẩu
                if (!dbPassword.equals(password)) {
                    throw new Exception("Sai tên đăng nhập hoặc mật khẩu!");
                }
                // Lấy quyền (Role) từ database
                String roleStr = rs.getString("role");
                Role dbRole = Role.valueOf(roleStr);
                // Kiểm tra xem có đúng quyền đăng nhập mong muốn không (Staff hay Customer)
                if (dbRole != expectedRole) {
                    throw new Exception("Tài khoản này không thuộc quyền truy cập '" + expectedRole.getDisplayName() + "'!");
                }
                // Khởi tạo đối tượng User từ dữ liệu SQL
                u = new User(
                    rs.getString("username"),
                    dbPassword,
                    rs.getString("full_name"),
                    dbRole,
                    rs.getString("phone"),
                    rs.getString("cccd")
                );
            } else {
                throw new Exception("Sai tên đăng nhập hoặc mật khẩu!");
            }
        } catch (SQLException e) {
            throw new Exception("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
        }
        this.currentUser = u;
        return u;
    }
    public User getCurrentUser() { 
        return currentUser; 
    }

    public void logout() { 
        currentUser = null; 
    }
}
