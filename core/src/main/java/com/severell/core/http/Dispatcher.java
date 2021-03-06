package com.severell.core.http;

import com.severell.core.container.Container;
import com.severell.core.error.ErrorHandler;
import com.severell.core.exceptions.ControllerException;
import com.severell.core.exceptions.MiddlewareException;
import com.severell.core.exceptions.NotFoundException;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * The Dispatcher handles all incoming request and dispatches them to the correct handlers.
 */
public class Dispatcher {

    private final Container c;
    private Router router;
    private final ErrorHandler errorHandler;

    /**
     * Creates a new Dispatcher
     *
     * @param c The {@link Container} used for any dependency resolution.
     */
    public Dispatcher(Container c) {
        this.c =c;

        this.errorHandler = c.make(ErrorHandler.class);
    }

    /**
     * Used to retrieve the router from the Container and set it.
     */
    public void initRouter(){
        this.router = c.make(Router.class);
    }

    /**
     * Takes a Request & Response object from the underlying server (i.e Jetty) and dispatches
     * to the correct handler.
     *
     * @param request Request object
     * @param response Response object
     */
    public void dispatch(Request request, Response response) {
        try {
           this.doRequest(request, response);
        } catch (Exception e) {
            errorHandler.handle(e, request, response);
        } finally {
            try{
                response.getWriter().close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                //This needs to be changed
            }
        }
    }

    /**
     * Finds the correct route and pass the request to the {@link MiddlewareManager} to pass through
     * the Middleware chain.
     *
     * @param request Request object
     * @param response Response object
     * @throws MiddlewareException
     * @throws ControllerException
     */
    private void doRequest(Request request, Response response) throws Exception {
        RouteExecutor ref = router.lookup(request.getRequestURI(), request.getMethod(), request);

        if(ref != null) {
            MiddlewareManager manager = new MiddlewareManager(ref, c);
            manager.filterRequest(request, response);
        } else {
            errorHandler.handle(new NotFoundException("No route has been configured for the given request "), request, response);
        }
    }
}
