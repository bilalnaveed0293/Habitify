<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../../config/database.php';
require_once '../../includes/functions.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        "status" => "error",
        "message" => "Method not allowed"
    ]);
    exit();
}

$data = json_decode(file_get_contents("php://input"), true);

if (!$data || !isset($data['email'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Email is required"
    ]);
    exit();
}

$email = sanitizeInput($data['email']);

if (!validateEmail($email)) {
    echo json_encode([
        "status" => "error", 
        "message" => "Invalid email format"
    ]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

// Check if user exists
$query = "SELECT id, name, email, phone FROM users WHERE email = :email AND is_active = 1";
$stmt = $db->prepare($query);
$stmt->bindParam(":email", $email);
$stmt->execute();

if ($stmt->rowCount() === 0) {
    echo json_encode([
        "status" => "error",
        "message" => "No account found with this email"
    ]);
    exit();
}

$user = $stmt->fetch(PDO::FETCH_ASSOC);
$userId = $user['id'];
$userName = $user['name'];
$userPhone = $user['phone'];

// Generate a 6-digit reset code
$resetCode = sprintf("%06d", mt_rand(0, 999999));

// Set expiration (5 minutes from now)
$expiresAt = date('Y-m-d H:i:s', strtotime('+5 minutes'));

// Delete old reset codes for this user
$deleteOld = "DELETE FROM password_resets WHERE user_id = :user_id";
$deleteStmt = $db->prepare($deleteOld);
$deleteStmt->bindParam(":user_id", $userId);
$deleteStmt->execute();

// Insert new reset code
$insertQuery = "INSERT INTO password_resets (user_id, reset_code, expires_at) 
                VALUES (:user_id, :reset_code, :expires_at)";
$insertStmt = $db->prepare($insertQuery);

$insertStmt->bindParam(":user_id", $userId);
$insertStmt->bindParam(":reset_code", $resetCode);
$insertStmt->bindParam(":expires_at", $expiresAt);

if ($insertStmt->execute()) {
    // Send email with reset code
    $emailSent = sendResetEmail($email, $userName, $resetCode);
    
    // Send SMS if phone number exists (optional)
    $smsSent = false;
    if (!empty($userPhone)) {
        $smsSent = sendResetSMS($userPhone, $resetCode);
    }
    
    // Return success response
    echo json_encode([
        "status" => "success",
        "message" => "Reset code sent successfully",
        "data" => [
            "email" => $email,
            "email_sent" => $emailSent,
            "sms_sent" => $smsSent,
            "code_expires_in" => "5 minutes"
        ]
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Failed to generate reset code"
    ]);
}

function sendResetEmail($toEmail, $userName, $resetCode) {
    $subject = "Habitify - Password Reset Code";
    
    // HTML email template
    $message = '
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            .container { max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif; }
            .header { background: #6A5ACD; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
            .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
            .code { font-size: 32px; font-weight: bold; color: #6A5ACD; text-align: center; margin: 20px 0; padding: 15px; background: white; border-radius: 8px; letter-spacing: 5px; }
            .footer { margin-top: 30px; font-size: 12px; color: #666; text-align: center; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h2>Habitify Password Reset</h2>
            </div>
            <div class="content">
                <p>Hello ' . htmlspecialchars($userName) . ',</p>
                <p>You have requested to reset your password for your Habitify account.</p>
                <p>Use the verification code below:</p>
                <div class="code">' . $resetCode . '</div>
                <p>This code will expire in 5 minutes.</p>
                <p>If you didn\'t request this, please ignore this email.</p>
                <div class="footer">
                    <p>This is an automated message, please do not reply.</p>
                    <p>&copy; ' . date('Y') . ' Habitify. All rights reserved.</p>
                </div>
            </div>
        </div>
    </body>
    </html>';
    
    $headers = "MIME-Version: 1.0" . "\r\n";
    $headers .= "Content-type:text/html;charset=UTF-8" . "\r\n";
    $headers .= 'From: Habitify <noreply@habitify.com>' . "\r\n";
    
    // For XAMPP localhost, you need to configure SMTP in php.ini
    // Or use PHPMailer for better email delivery
    
    // Simulate email sending for development
    // In production, uncomment the mail() function
    // return mail($toEmail, $subject, $message, $headers);
    
    // Log email for development
    error_log("Password reset email to $toEmail: Code = $resetCode");
    
    // Return true for simulation
    return true;
}

function sendResetSMS($phoneNumber, $resetCode) {
    // This function requires an SMS gateway API like Twilio, Nexmo, etc.
    // For now, we'll simulate it
    
    // Example with Twilio (you would need to install Twilio SDK)
    /*
    require_once 'vendor/autoload.php';
    
    $sid = 'your_twilio_sid';
    $token = 'your_twilio_token';
    $twilio = new Twilio\Rest\Client($sid, $token);
    
    $message = $twilio->messages->create(
        $phoneNumber,
        [
            'from' => '+1234567890',
            'body' => "Your Habitify verification code is: $resetCode"
        ]
    );
    
    return $message->sid ? true : false;
    */
    
    // Log SMS for development
    error_log("SMS to $phoneNumber: Verification code = $resetCode");
    
    // Return true for simulation
    return true;
}
?>