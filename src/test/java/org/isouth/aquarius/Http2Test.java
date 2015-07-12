package org.isouth.aquarius;

import io.netty.example.http2.helloworld.server.Http2Server;
import org.isouth.App;
import org.isouth.Aquarius;

import static org.isouth.Aquarius.*;

/**
 * Created by qiyi on 7/11/2015.
 */
public class Http2Test {

    public static void main(String[] args) throws Exception {
        Aquarius aquarius = new Aquarius();
        aquarius.route("/", App.class);
        run(null, "localhost", 8443);
    }
}
