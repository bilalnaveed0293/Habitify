-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Dec 05, 2025 at 11:50 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `habitify_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `custom_habits`
--

CREATE TABLE `custom_habits` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `category` varchar(50) DEFAULT 'custom',
  `icon_name` varchar(50) DEFAULT 'default',
  `color_code` varchar(7) DEFAULT '#4CAF50',
  `frequency` enum('daily','weekly','custom') DEFAULT 'daily',
  `reminder_time` time DEFAULT '09:00:00',
  `reminder_enabled` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_active` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `custom_habits`
--

INSERT INTO `custom_habits` (`id`, `user_id`, `title`, `description`, `category`, `icon_name`, `color_code`, `frequency`, `reminder_time`, `reminder_enabled`, `created_at`, `is_active`) VALUES
(1, 1, 'Read 100 Pages', 'Read a book for personal growth', 'learning', 'read', '#4CAF50', 'daily', '06:30:00', 1, '2025-12-05 22:24:33', 1),
(2, 1, 'Crashout gng', 'u deserve it', 'mindfulness', 'gratitude', '#F44336', 'daily', '09:00:00', 1, '2025-12-05 22:48:24', 1),
(3, 1, 'Stuff', 'Haha', 'custom', 'water', '#4CAF50', 'daily', '09:00:00', 1, '2025-12-05 22:49:10', 1);

-- --------------------------------------------------------

--
-- Table structure for table `habits`
--

CREATE TABLE `habits` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `frequency` enum('daily','weekly','custom') DEFAULT 'daily',
  `custom_frequency_days` int(11) DEFAULT 1,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `color_code` varchar(7) DEFAULT '#4CAF50',
  `icon_name` varchar(50) DEFAULT 'default',
  `current_streak` int(11) DEFAULT 0,
  `longest_streak` int(11) DEFAULT 0,
  `status` enum('todo','completed','failed','archived') DEFAULT 'todo',
  `is_active` tinyint(1) DEFAULT 1,
  `reminder_time` time DEFAULT '09:00:00',
  `reminder_enabled` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `local_id` varchar(100) DEFAULT NULL,
  `sync_status` enum('synced','pending','conflict') DEFAULT 'synced'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `habits`
--

INSERT INTO `habits` (`id`, `user_id`, `title`, `description`, `frequency`, `custom_frequency_days`, `start_date`, `end_date`, `color_code`, `icon_name`, `current_streak`, `longest_streak`, `status`, `is_active`, `reminder_time`, `reminder_enabled`, `created_at`, `updated_at`, `local_id`, `sync_status`) VALUES
(13, 1, 'Drink 8 Glasses of Water', 'Stay hydrated throughout the day', 'daily', 1, '2025-12-06', NULL, '#2196F3', 'water', 0, 1, 'failed', 1, '09:00:00', 1, '2025-12-05 21:41:17', '2025-12-05 21:52:31', NULL, 'synced'),
(15, 1, 'Early to Bed', 'Go to bed before 11 PM', 'daily', 1, '2025-12-05', NULL, '#3F51B5', 'sleep', 1, 2, 'todo', 1, '09:00:00', 1, '2025-12-05 21:59:40', '2025-12-05 21:59:57', NULL, 'synced'),
(16, 1, 'Practice Gratitude', 'Write down 3 things you are grateful for', 'daily', 1, '2025-12-05', NULL, '#FFC107', 'gratitude', 0, 0, 'todo', 1, '09:00:00', 1, '2025-12-05 22:05:24', '2025-12-05 22:05:24', NULL, 'synced'),
(17, 1, 'Morning Meditation', 'Start your day with 10 minutes of meditation', 'daily', 1, '2025-12-05', NULL, '#4CAF50', 'meditation', 0, 0, 'todo', 1, '09:00:00', 1, '2025-12-05 22:22:25', '2025-12-05 22:22:25', NULL, 'synced'),
(18, 1, 'Morning Meditation', 'Start your day with 10 minutes of meditation', 'daily', 1, '2025-12-05', NULL, '#4CAF50', 'meditation', 0, 0, 'todo', 1, '09:00:00', 1, '2025-12-05 22:48:40', '2025-12-05 22:48:40', NULL, 'synced'),
(19, 1, 'Stuff', 'Haha', 'daily', 1, '2025-12-05', NULL, '#4CAF50', 'water', 0, 0, 'todo', 1, '09:00:00', 1, '2025-12-05 22:49:10', '2025-12-05 22:49:10', NULL, 'synced');

-- --------------------------------------------------------

--
-- Table structure for table `habit_logs`
--

CREATE TABLE `habit_logs` (
  `id` int(11) NOT NULL,
  `habit_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `log_date` date NOT NULL,
  `status` enum('todo','completed','failed','skipped') DEFAULT 'todo',
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `habit_logs`
--

INSERT INTO `habit_logs` (`id`, `habit_id`, `user_id`, `log_date`, `status`, `completed_at`, `created_at`, `updated_at`) VALUES
(13, 13, 1, '2025-12-06', 'todo', NULL, '2025-12-05 21:41:17', '2025-12-05 21:41:17'),
(14, 13, 1, '2025-12-05', 'failed', '2025-12-05 21:52:31', '2025-12-05 21:41:25', '2025-12-05 21:52:31'),
(16, 15, 1, '2025-12-05', 'completed', '2025-12-05 21:59:47', '2025-12-05 21:59:40', '2025-12-05 21:59:47'),
(17, 16, 1, '2025-12-05', 'todo', NULL, '2025-12-05 22:05:24', '2025-12-05 22:05:24'),
(18, 17, 1, '2025-12-05', 'todo', NULL, '2025-12-05 22:22:25', '2025-12-05 22:22:25'),
(19, 18, 1, '2025-12-05', 'todo', NULL, '2025-12-05 22:48:40', '2025-12-05 22:48:40'),
(20, 19, 1, '2025-12-05', 'todo', NULL, '2025-12-05 22:49:10', '2025-12-05 22:49:10');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `habit_id` int(11) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `message` text NOT NULL,
  `type` enum('habit_reminder','streak_milestone','motivation') DEFAULT 'habit_reminder',
  `scheduled_time` datetime NOT NULL,
  `is_sent` tinyint(1) DEFAULT 0,
  `sent_at` timestamp NULL DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `password_change_logs`
--

CREATE TABLE `password_change_logs` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `change_date` datetime NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `password_change_logs`
--

INSERT INTO `password_change_logs` (`id`, `user_id`, `change_date`, `ip_address`) VALUES
(1, 1, '2025-12-05 17:16:51', '192.168.100.71');

-- --------------------------------------------------------

--
-- Table structure for table `password_resets`
--

CREATE TABLE `password_resets` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `reset_code` varchar(6) DEFAULT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `password_resets`
--

INSERT INTO `password_resets` (`id`, `user_id`, `reset_code`, `expires_at`, `created_at`) VALUES
(1, 1, '965575', '2025-12-03 00:55:48', '2025-12-02 18:25:48'),
(4, 3, '598389', '2025-12-03 00:52:46', '2025-12-02 18:47:46');

-- --------------------------------------------------------

--
-- Table structure for table `predefined_habits`
--

CREATE TABLE `predefined_habits` (
  `id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `category` varchar(50) DEFAULT 'general',
  `icon_name` varchar(50) DEFAULT 'default',
  `color_code` varchar(7) DEFAULT '#4CAF50',
  `frequency` enum('daily','weekly','custom') DEFAULT 'daily',
  `suggested_count` int(11) DEFAULT 0,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `predefined_habits`
--

INSERT INTO `predefined_habits` (`id`, `title`, `description`, `category`, `icon_name`, `color_code`, `frequency`, `suggested_count`, `is_active`, `created_at`) VALUES
(1, 'Morning Meditation', 'Start your day with 10 minutes of meditation', 'mindfulness', 'meditation', '#4CAF50', 'daily', 0, 1, '2025-12-05 08:53:46'),
(2, 'Drink 8 Glasses of Water', 'Stay hydrated throughout the day', 'health', 'water', '#2196F3', 'daily', 2, 1, '2025-12-05 08:53:46'),
(3, 'Exercise for 30 mins', 'Daily physical activity for better health', 'fitness', 'exercise', '#FF9800', 'daily', 1, 1, '2025-12-05 08:53:46'),
(4, 'Read 20 Pages', 'Read a book for personal growth', 'learning', 'read', '#9C27B0', 'daily', 1, 1, '2025-12-05 08:53:46'),
(5, 'Journal Writing', 'Write down your thoughts and reflections', 'mindfulness', 'journal', '#FF5722', 'daily', 1, 1, '2025-12-05 08:53:46'),
(6, 'Learn Something New', 'Spend 30 minutes learning a new skill', 'learning', 'learn', '#673AB7', 'daily', 1, 1, '2025-12-05 08:53:46'),
(7, 'No Sugar', 'Avoid sugar for better health', 'health', 'nosugar', '#F44336', 'daily', 0, 1, '2025-12-05 08:53:46'),
(8, 'Early to Bed', 'Go to bed before 11 PM', 'health', 'sleep', '#3F51B5', 'daily', 1, 1, '2025-12-05 08:53:46'),
(9, 'Practice Gratitude', 'Write down 3 things you are grateful for', 'mindfulness', 'gratitude', '#FFC107', 'daily', 0, 1, '2025-12-05 08:53:46'),
(10, 'Walk 10,000 Steps', 'Achieve daily step goal', 'fitness', 'walk', '#009688', 'daily', 1, 1, '2025-12-05 08:53:46');

-- --------------------------------------------------------

--
-- Table structure for table `sync_logs`
--

CREATE TABLE `sync_logs` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `table_name` varchar(50) NOT NULL,
  `record_id` int(11) NOT NULL,
  `operation` enum('insert','update','delete') NOT NULL,
  `local_id` varchar(100) DEFAULT NULL,
  `sync_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_conflict` tinyint(1) DEFAULT 0,
  `resolved_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `sync_logs`
--

INSERT INTO `sync_logs` (`id`, `user_id`, `table_name`, `record_id`, `operation`, `local_id`, `sync_timestamp`, `is_conflict`, `resolved_at`) VALUES
(1, 1, 'habits', 1, 'delete', NULL, '2025-12-05 21:32:14', 0, NULL),
(2, 1, 'habits', 1, 'delete', NULL, '2025-12-05 21:38:24', 0, NULL),
(3, 1, 'habits', 1, 'delete', NULL, '2025-12-05 21:38:59', 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `theme` enum('light','dark','system') DEFAULT 'system',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `last_sync` timestamp NULL DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `name`, `email`, `password_hash`, `phone`, `profile_picture`, `theme`, `created_at`, `updated_at`, `last_sync`, `is_active`) VALUES
(1, 'Ayaan Mughal', 'a@m.c', '$2y$10$7RWljaKE9YqdeGJ17NhlzOljQUlMavAvbhS5QsBfxrBU4Wogw83la', '03152092828', 'uploads/profile_pictures/profile_1_1764972295_92a4b31ba4e3e538.jpg', 'system', '2025-12-02 18:04:20', '2025-12-05 22:04:55', '2025-12-05 22:04:55', 1),
(2, 'Test User', 'a@m.b', '$2y$10$qPTOME8ZyMlBSFs8dtPWkeVMOhjg/DWgEuCCELpDb2wLiRy2M.NTm', NULL, NULL, 'system', '2025-12-02 18:08:39', '2025-12-02 18:08:39', NULL, 1),
(3, 'Test User', 'ayaanmughal03@gmail.com', '$2y$10$AbJKESIH2xo8sWOZALryx.cLdu0PG8N1X55hLmycH.sOm2f1F8EtK', NULL, NULL, 'system', '2025-12-02 18:42:25', '2025-12-02 18:42:25', NULL, 1);

-- --------------------------------------------------------

--
-- Table structure for table `user_settings`
--

CREATE TABLE `user_settings` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `notifications_enabled` tinyint(1) DEFAULT 1,
  `reminder_time` time DEFAULT '09:00:00',
  `weekly_report` tinyint(1) DEFAULT 1,
  `language` varchar(10) DEFAULT 'en',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_settings`
--

INSERT INTO `user_settings` (`id`, `user_id`, `notifications_enabled`, `reminder_time`, `weekly_report`, `language`, `created_at`, `updated_at`) VALUES
(1, 2, 1, '09:00:00', 1, 'en', '2025-12-02 18:08:39', '2025-12-02 18:08:39'),
(2, 3, 1, '09:00:00', 1, 'en', '2025-12-02 18:42:25', '2025-12-02 18:42:25');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `custom_habits`
--
ALTER TABLE `custom_habits`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `habits`
--
ALTER TABLE `habits`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `habit_logs`
--
ALTER TABLE `habit_logs`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_habit_date` (`habit_id`,`log_date`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `habit_id` (`habit_id`);

--
-- Indexes for table `password_change_logs`
--
ALTER TABLE `password_change_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_change_date` (`change_date`);

--
-- Indexes for table `password_resets`
--
ALTER TABLE `password_resets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user` (`user_id`),
  ADD KEY `idx_expires` (`expires_at`);

--
-- Indexes for table `predefined_habits`
--
ALTER TABLE `predefined_habits`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `sync_logs`
--
ALTER TABLE `sync_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_sync` (`user_id`,`sync_timestamp`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_email` (`email`);

--
-- Indexes for table `user_settings`
--
ALTER TABLE `user_settings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `custom_habits`
--
ALTER TABLE `custom_habits`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `habits`
--
ALTER TABLE `habits`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `habit_logs`
--
ALTER TABLE `habit_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `password_change_logs`
--
ALTER TABLE `password_change_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `password_resets`
--
ALTER TABLE `password_resets`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `predefined_habits`
--
ALTER TABLE `predefined_habits`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `sync_logs`
--
ALTER TABLE `sync_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `user_settings`
--
ALTER TABLE `user_settings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `custom_habits`
--
ALTER TABLE `custom_habits`
  ADD CONSTRAINT `custom_habits_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `habits`
--
ALTER TABLE `habits`
  ADD CONSTRAINT `habits_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `habit_logs`
--
ALTER TABLE `habit_logs`
  ADD CONSTRAINT `habit_logs_ibfk_1` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `habit_logs_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `notifications_ibfk_2` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `password_change_logs`
--
ALTER TABLE `password_change_logs`
  ADD CONSTRAINT `password_change_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `sync_logs`
--
ALTER TABLE `sync_logs`
  ADD CONSTRAINT `sync_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `user_settings`
--
ALTER TABLE `user_settings`
  ADD CONSTRAINT `user_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
