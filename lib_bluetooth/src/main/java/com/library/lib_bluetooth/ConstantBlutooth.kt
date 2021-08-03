package com.library.lib_bluetooth

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/10
 * @Describe:
 */
//未处理
const val WAIT = 0x0000
//不支持蓝牙
const val NOT_SUPPORTED_BLUETOOTH = 0x0001

//未开启蓝牙
const val NOT_TURN_ON = 0x0002

//连接中
const val CONNECTTING = 0x0003

//连接成功
const val CONNNECT_SUCCESS = 0x0004

//连接失败
const val CONNNECT_FAILED = 0x0005

//蓝牙状态
const val BLUETOOTH_STATE = 0x00a1
const val STATE_DATA = "STATE_DATA"
//remote_mac
const val BLUETOOTH_REMOTE_MAC = 0x00a2
const val REMOTE_MAC = "REMOTE_MAC"
//read msg
const val BLUETOOTH_RECEIVE_MSG = 0x00a3
const val RECEIVE_MSG = "RECEIVE_MSG"