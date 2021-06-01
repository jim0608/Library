package com.mysafe.lib_serial_port

/**
 * @author Create By 张晋铭
 * @Date on 2021/3/19
 * @Describe:串口连接中的tag标签
 */


//region 获取完整串口数据
/**
 * 获取的串口数据开头部分
 */
const val TAG_READING_START = 0x00A1

/**
 * 获取的串口数据中间部分
 */
const val TAG_READING_CENTRE = 0x00A2

/**
 * 获取的串口数据结尾部分
 */
const val TAG_READING_END = 0x00A3

/**
 * 简单数据，read线程读取一次就获取完毕
 */
const val TAG_READING_CONTENT = 0x00A4

/**
 * 发送串口未携带数据
 */
const val TAG_READING_EMPTY = 0x00A5
//endregion

//region 串口读取当前状态
/**
 * 读操作等待中，等待串口数据发送
 */
const val TAG_READING_WAITING = 0x00B1

/**
 * 读操作工作中，正在读取串口数据
 */
const val TAG_READING_WORKING = 0x00B2

/**
 * 读操作完成，将串口数据读取完毕
 */
const val TAG_READING_FINISH = 0x00B3

//endregion

//region 串口发送的数据类型
//TAG_SERIAL_OVER: 192
//TAG_SEND_OVER: 193
//TAG_SERIAL_CONNECT: 194
//TAG_SERIAL_FACE: 195
//TAG_SERIAL_MEALS: 196
//TAG_SERIAL_FINISH_FOOD: 197
//TAG_SERIAL_ERROR: 198
/**
 * 串口连接结束
 */
const val TAG_SERIAL_OVER = 0x00C0

/**
 * 数据发送成功
 */
const val TAG_SEND_OVER = 0x00C1

/**
 * 配餐端发起连接，并通过串口同步数据
 */
const val TAG_SERIAL_CONNECT = 0x00C2

/**
 * 取餐端发送人脸数据
 */
const val TAG_SERIAL_FACE = 0x00C3

/**
 * 获取订餐数据完成，两端显示数据
 */
const val TAG_SERIAL_MEALS = 0x00C4

/**
 * 配餐完成
 */
const val TAG_SERIAL_FINISH_FOOD = 0x00C5

/**
 * 配餐出错
 */
const val TAG_SERIAL_ERROR = 0x00C6
//endregion

//region 串口发送数据标志位
/**
 * 串口发送json数据的tag 与content标签
 */
const val KEY_TAG = "Tag"
const val KEY_CONTENT = "Content"

//防沾包串口数据前缀与后缀（前缀）
const val TAG_BEGINNING = "#_Begin_#"

// （前缀）16进制：23 48 61 6A 69 6D 65 23
const val TAG_BEGINNING_HEX = "235F426567696E5F23"

//防沾包串口数据前缀与后缀（后缀）
const val TAG_ENDING = "#_Ending_#"

// （后缀）16进制：23 4F 77 61 72 69 23
const val TAG_ENDING_HEX = "235F456E64696E675F23"

/**
 * 数据粘合
 */
const val TAG_BE_GLUED = TAG_ENDING + TAG_BEGINNING
const val TAG_BE_GLUED_HEX = TAG_ENDING_HEX + TAG_BEGINNING_HEX

//endregion