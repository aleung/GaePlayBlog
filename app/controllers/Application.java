package controllers;

import play.modules.gae.GAE;
import play.mvc.Controller;
import play.mvc.results.Redirect;

public class Application extends Controller {

    public static void index() {
        Blog.showList();
    }

    public static void login(String returnUrl) {
        String url = GAE.getUserService().createLoginURL(returnUrl);
        throw new Redirect(url);
    }

    public static void logout(String returnUrl) {
        String url = GAE.getUserService().createLogoutURL(returnUrl);
        throw new Redirect(url);
    }

}
