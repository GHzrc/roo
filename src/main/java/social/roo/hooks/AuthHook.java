package social.roo.hooks;

import com.blade.ioc.annotation.Bean;
import com.blade.kit.StringKit;
import com.blade.mvc.hook.Signature;
import com.blade.mvc.hook.WebHook;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.http.Session;
import lombok.extern.slf4j.Slf4j;
import social.roo.RooConst;
import social.roo.auth.Access;
import social.roo.enums.UserRole;
import social.roo.model.dto.Auth;
import social.roo.model.entity.User;
import social.roo.utils.RooUtils;

import java.lang.reflect.Method;

import static social.roo.RooConst.LOGIN_SESSION_KEY;

/**
 * 登录权限验证中间件
 *
 * @author biezhi
 * @date 2017/8/6
 */
@Slf4j
@Bean
public class AuthHook implements WebHook {

    @Override
    public boolean before(Signature signature) {
        Request request = signature.request();
        String  uri     = request.uri();

        log.info("{}\t{}", request.method(), uri);

        Method method = signature.getAction();
        Access access = method.getAnnotation(Access.class);
        if (null == access) {
            return true;
        }

        Response response = signature.response();
        User     user     = Auth.loginUser();
        if (null == user) {
            user = Auth.getUserByCookie();
            if (null != user) {
                request.session().attribute(LOGIN_SESSION_KEY, user);
            }
        }

        UserRole userRole = access.value();

        // 未登录
        if (userRole.getId() > 0 && null == user) {
            signature.response().redirect("/signin");
            return false;
        }

        // 无权限操作
        if (null != user) {
            UserRole userRoleTemp = UserRole.valueOf(user.getRole().toUpperCase());
            if (userRoleTemp.getId() < userRole.getId()) {
                response.badRequest().text("Permission denied");
                return false;
            }
        }

        return true;
    }

}