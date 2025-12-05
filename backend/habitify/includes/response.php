<?php
class Response {
    public static function send($status, $message, $data = null, $code = 200) {
        http_response_code($code);
        
        $response = [
            "success" => $status == "success",
            "message" => $message,
            "timestamp" => date('Y-m-d H:i:s')
        ];
        
        if ($data !== null) {
            $response["data"] = $data;
        }
        
        echo json_encode($response);
        exit();
    }
    
    public static function error($message, $code = 400) {
        self::send("error", $message, null, $code);
    }
    
    public static function success($message, $data = null, $code = 200) {
        self::send("success", $message, $data, $code);
    }
}
?>