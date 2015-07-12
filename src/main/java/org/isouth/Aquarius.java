package org.isouth;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.io.InputStream;
import java.lang.annotation.*;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * Created by qiyi on 7/11/2015.
 */
public class Aquarius {

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Routes.class)
    public @interface Route {
        String value();

        String method() default "GET";
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Routes {
        Route[] value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Gets.class)
    public @interface Get {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Gets {
        Get[] value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Posts.class)
    public @interface Post {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Posts {
        Post[] value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Puts.class)
    public @interface Put {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Puts {
        Put[] value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Heads.class)
    public @interface Head {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Heads {
        Head[] value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Deletes.class)
    public @interface Delete {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Deletes {
        Delete[] value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Error {
        int value() default 200;
    }


    public static class Request {

        public static Request me() {
            return new Request();
        }

        public Map<String, String> forms() {
            return null;
        }

        public Map<String, String> headers() {
            return null;
        }

        public String getHeader() {
            return null;
        }

        public InputStream files() {
            return null;
        }

        public String getCookie(String name) {
            return null;
        }

        public String query(String name) {
            return null;
        }

        public Object body() {
            return null;
        }

        public Map<String, String> environ() {
            return null;
        }
    }

    public static class Response extends Request {

        public Response setHeader() {
            return this;
        }

        public Response addHeader() {
            return this;
        }

        public Response setCookie(String key, String value) {
            return this;
        }

    }

    private static Aquarius defaultAquarius;

    public Aquarius route(String path, Class<?> subApp) {


        return this;
    }

    public static class AquariusHttp2Listener extends Http2FrameAdapter {
        Http2ConnectionEncoder encoder;

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                  Http2Headers headers, int streamDependency, short weight,
                                  boolean exclusive, int padding, boolean endStream) throws Http2Exception {
            if (endStream) {
                Http2Headers rspHeaders = new DefaultHttp2Headers().status(OK.codeAsText());
                ByteBuf content = ctx.alloc().buffer();
                ByteBufUtil.writeUtf8(content, "http2, -.-");
                encoder.writeHeaders(ctx, streamId, rspHeaders, 0, false, ctx.newPromise());
                encoder.writeData(ctx, streamId, content, 0, true, ctx.newPromise());
                ctx.flush();
                System.out.print("Hello, got it.");
            }
        }
    }

    public static void run(Aquarius app, String host, int port) throws CertificateException, SSLException, InterruptedException {
        final SslContext sslCtx;
        SslProvider sslProvider = SslProvider.JDK;
        SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
        sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(sslProvider)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2))
                .build();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                            AquariusHttp2Listener http2listener = new AquariusHttp2Listener();
                            Http2ConnectionHandler http2ConnectionHandler = new Http2ConnectionHandler(true, http2listener);
                            http2listener.encoder = http2ConnectionHandler.encoder();
                            pipeline.addLast(http2ConnectionHandler);
                        }
                    });

            Channel ch = b.bind(host, port).sync().channel();
            System.err.println("Open your HTTP/2-enabled web browser and navigate to https://" + host + ":" + port + "/");

            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
