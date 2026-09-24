# 补齐单会话登录

sys_user.current_session_id 保存当前会话。JwtUtil 增加 sid claim，所有登录签发点先写会话再签发。鉴权同时校验签名、账号状态和 sid；Wiki 协作连接也检查当前会话。部署前通过 Jenkins 执行增量 SQL，再部署 develop；既有无 sid 的令牌需重新登录。
