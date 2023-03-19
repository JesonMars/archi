/*
 Navicat Premium Data Transfer

 Source Server         : localhost-docker-mysql-3307
 Source Server Type    : MySQL
 Source Server Version : 50729
 Source Host           : 127.0.0.1:3307
 Source Schema         : db_gateway

 Target Server Type    : MySQL
 Target Server Version : 50729
 File Encoding         : 65001

 Date: 19/03/2023 23:15:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_gateway
-- ----------------------------
DROP TABLE IF EXISTS `t_gateway`;
CREATE TABLE `t_gateway` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `router_code` varchar(30) DEFAULT NULL COMMENT '路由id',
  `path` varchar(100) DEFAULT NULL COMMENT '要拦截的url',
  `segment` varchar(50) DEFAULT NULL COMMENT '要匹配的url后缀',
  `router_uri` varchar(100) DEFAULT NULL COMMENT '要转发到的uri',
  `crud_code` varchar(60) DEFAULT NULL COMMENT '数据库操作code',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint(4) DEFAULT '0',
  `create_user` varchar(60) DEFAULT NULL COMMENT '创建用户',
  `update_user` varchar(60) DEFAULT NULL COMMENT '更新用户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_gateway
-- ----------------------------
BEGIN;
INSERT INTO `t_gateway` VALUES (1, 'gateway_test', '/test/**', NULL, 'http://127.0.0.1:8009/', NULL, '2023-03-19 09:52:20', '2023-03-19 10:29:51', 0, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_gateway_filter
-- ----------------------------
DROP TABLE IF EXISTS `t_gateway_filter`;
CREATE TABLE `t_gateway_filter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `router_code` varchar(20) DEFAULT NULL COMMENT '过滤器id',
  `filter_name` varchar(100) DEFAULT NULL COMMENT '过滤器名称',
  `filter_args` varchar(255) DEFAULT NULL COMMENT '过滤器参数,json字符串',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint(4) NOT NULL DEFAULT '0',
  `order_by` int(1) DEFAULT '0' COMMENT 'router_code内部排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_gateway_filter
-- ----------------------------
BEGIN;
INSERT INTO `t_gateway_filter` VALUES (1, NULL, 'AddRequestParameter', NULL, '2020-12-16 11:32:50', '2023-03-19 09:55:34', 0, 0);
INSERT INTO `t_gateway_filter` VALUES (2, NULL, 'AddRequestHeader', '{\"_genkey_0\":\"header\",\"_genkey_1\":\"addHeader\"}', '2020-12-17 17:57:53', '2023-03-19 09:55:39', 0, 0);
INSERT INTO `t_gateway_filter` VALUES (3, NULL, 'GatewayContext', NULL, '2020-12-17 17:58:25', '2023-03-19 09:55:51', 0, 0);
INSERT INTO `t_gateway_filter` VALUES (6, 'wechat_login', 'Login', '{\"key\":\"wechatOpenId\"}', '2020-12-17 17:48:42', '2023-03-19 09:55:48', 0, 0);
COMMIT;

-- ----------------------------
-- Table structure for t_gateway_thirdjar
-- ----------------------------
DROP TABLE IF EXISTS `t_gateway_thirdjar`;
CREATE TABLE `t_gateway_thirdjar` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT 'jar包全名称,不包括后缀和版本号',
  `artifact_id` varchar(50) DEFAULT NULL,
  `group_id` varchar(50) DEFAULT NULL,
  `version` varchar(20) DEFAULT NULL COMMENT '版本号',
  `download_url` text COMMENT '要下载的jar包url',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
