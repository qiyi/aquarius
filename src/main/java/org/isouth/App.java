package org.isouth;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.isouth.Aquarius.*;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    @Route("/")
    @Route("/home")
    public String get() {
        return "Home Page";
    }

    @Route(value = "/<page_name>", method = "GET")
    public String showPage(String pageName) {
        Request request = Request.me();
        return null;
    }

    @Get("/login")
    public String login(Request request) {
        String username = request.forms().get("username");

        return null;
    }

    @Post("/post")
    public Response newPost() {
        return null;
    }


}
