package com.yuan.miaosha.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yuan.miaosha.dao.MiaoshaUserDao;
import com.yuan.miaosha.domain.MiaoshaUser;
import com.yuan.miaosha.exception.GlobalException;
import com.yuan.miaosha.redis.MiaoshaUserKey;
import com.yuan.miaosha.redis.RedisService;
import com.yuan.miaosha.result.CodeMsg;
import com.yuan.miaosha.util.MD5Util;
import com.yuan.miaosha.util.UUIDUtil;
import com.yuan.miaosha.vo.LoginVo;

@Service
public class MiaoshaUserService {
	
	
	public static final String COOKI_NAME_TOKEN = "token";
	
	@Autowired
	MiaoshaUserDao miaoshaUserDao;
	
	@Autowired
	RedisService redisService;
	
	public MiaoshaUser getById(long id) {
		//取缓存
		MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, ""+id, MiaoshaUser.class);
		if(user != null) {
			return user;
		}
		//取数据库
		user = miaoshaUserDao.getById(id);
		if(user != null) {
			redisService.set(MiaoshaUserKey.getById, ""+id, user);
		}
		return user;
	}
	// http://blog.csdn.net/tTU1EvLDeLFq5btqiK/article/details/78693323
	public boolean updatePassword(String token, long id, String formPass) {
		//取user
		MiaoshaUser user = getById(id);
		if(user == null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//更新数据库
		MiaoshaUser toBeUpdate = new MiaoshaUser();
		toBeUpdate.setId(id);
		toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));
		miaoshaUserDao.update(toBeUpdate);
		//处理缓存
		redisService.delete(MiaoshaUserKey.getById, ""+id);
		user.setPassword(toBeUpdate.getPassword());
		redisService.set(MiaoshaUserKey.token, token, user);
		return true;
	}


	public MiaoshaUser getByToken(HttpServletResponse response, String token) {
		if(StringUtils.isEmpty(token)) {
			return null;
		}
		MiaoshaUser user = redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
		//延长有效期
		if(user != null) {
			addCookie(response, token, user);
		}
		return user;
	}
	

	public String login(HttpServletResponse response, LoginVo loginVo) {
		if(loginVo == null) {
			throw new GlobalException(CodeMsg.SERVER_ERROR);
		}
		String mobile = loginVo.getMobile();
		String formPass = loginVo.getPassword();
		//判断手机号是否存在
		MiaoshaUser user = getById(Long.parseLong(mobile));
		if(user == null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//验证密码
		//数据库的密码
		String dbPass = user.getPassword();
		String saltDB = user.getSalt();
		//把客户端传进来的密码再进行一次MD5。再比较
		String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
		if(!calcPass.equals(dbPass)) {
			throw new GlobalException(CodeMsg.PASSWORD_ERROR);
		}
		//生成cookie，随机的token
		String token= UUIDUtil.uuid();
		//把token写入到cookie中，传递给客户端。
		//要标识这个token对应的是哪个用户，把用户信息写入到redis中。
		addCookie(response, token, user);
		return token;
	}
	
	private void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {
		//把私人信息存放到第三方的缓存中。从数据库取的用户信息user
		redisService.set(MiaoshaUserKey.token, token, user);
		//生成cookie。需要传（name，value）
		Cookie cookie = new Cookie(COOKI_NAME_TOKEN, token);
		//设置cookie有效期：为了跟session保持一致，就设置为MiaoshaUserKey.token的有效期。
		cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
		//设置到网站的根目录
		cookie.setPath("/");
		//写到response。就把cookie写到客户端去了。
		response.addCookie(cookie);
	}

}
