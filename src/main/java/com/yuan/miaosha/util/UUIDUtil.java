package com.yuan.miaosha.util;

import java.util.UUID;

public class UUIDUtil {
	public static String uuid() {
		//原生UUID有一个横杠，我们去掉这个横杠
		return UUID.randomUUID().toString().replace("-", "");
	}
}
