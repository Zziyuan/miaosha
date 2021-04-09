package com.yuan.miaosha.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.yuan.miaosha.access.UserContext;
import com.yuan.miaosha.domain.MiaoshaUser;
import com.yuan.miaosha.service.MiaoshaUserService;

@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

	@Autowired
	MiaoshaUserService userService;
	
	public boolean supportsParameter(MethodParameter parameter) {
		//获取参数类型
		Class<?> clazz = parameter.getParameterType();
		//如果这个类型是MiaoshaUser，就返回true。才会做下面的处理
		return clazz==MiaoshaUser.class;
	}

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		//这里面把token取出来，获取user。、
		//遍历request里面所有的cookie，找我们需要的哪个cookie。

		return UserContext.getUser();
	}

}
