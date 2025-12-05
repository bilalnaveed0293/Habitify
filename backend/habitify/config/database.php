<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

class Database {
    private $host = "localhost";
    private $db_name = "habitify_db";
    private $username = "root";  // Default XAMPP username
    private $password = "";      // Default XAMPP password (empty)
    public $conn;

    public function getConnection() {
        $this->conn = null;

        try {
            $this->conn = new PDO(
                "mysql:host=" . $this->host . ";dbname=" . $this->db_name,
                $this->username,
                $this->password
            );
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->conn->exec("set names utf8");
        } catch(PDOException $exception) {
            echo json_encode([
                "status" => "error",
                "message" => "Database connection error: " . $exception->getMessage()
            ]);
            exit();
        }

        return $this->conn;
    }
}
?>