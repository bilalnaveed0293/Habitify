<?php
require_once 'includes/response.php';

Response::success("Habitify API", [
    "api_endpoints" => [
        "Authentication" => [
            "POST /api/auth/register" => "Register new user",
            "POST /api/auth/login" => "Login user",
            "POST /api/auth/forgot_password" => "Request password reset"
        ],
        "Habits" => [
            "GET /api/habits/list" => "Get user habits",
            "POST /api/habits/create" => "Create new habit",
            "PUT /api/habits/update" => "Update habit",
            "DELETE /api/habits/delete" => "Delete habit"
        ],
        "Sync" => [
            "POST /api/sync/push" => "Push local changes to server",
            "GET /api/sync/pull" => "Pull server changes to local"
        ],
        "Profile" => [
            "GET /api/profile/get" => "Get user profile",
            "PUT /api/profile/update" => "Update profile"
        ]
    ],
    "status" => "API is running",
    "version" => "1.0.0",
    "timestamp" => date('Y-m-d H:i:s')
]);
?>