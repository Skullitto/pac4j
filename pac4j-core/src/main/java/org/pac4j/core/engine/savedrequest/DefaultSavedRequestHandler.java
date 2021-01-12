package org.pac4j.core.engine.savedrequest;

import org.pac4j.core.context.ContextHelper;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.*;
import org.pac4j.core.util.HttpActionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * The default {@link SavedRequestHandler} which handles GET and POST requests.
 *
 * @author Jerome LELEU
 * @since 4.0.0
 */
public class DefaultSavedRequestHandler implements SavedRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSavedRequestHandler.class);

    @Override
    public void save(final WebContext context, final SessionStore sessionStore) {
        final String requestedUrl = getRequestedUrl(context, sessionStore);
        if (ContextHelper.isPost(context)) {
            LOGGER.debug("requestedUrl with data: {}", requestedUrl);
            final String formPost = HttpActionHelper.buildFormPostContent(context);
            sessionStore.set(context, Pac4jConstants.REQUESTED_URL, new OkAction(formPost));
        } else {
            LOGGER.debug("requestedUrl: {}", requestedUrl);
            sessionStore.set(context, Pac4jConstants.REQUESTED_URL, new FoundAction(requestedUrl));
        }
    }

    protected String getRequestedUrl(final WebContext context, final SessionStore sessionStore) {
        return context.getFullRequestURL();
    }

    @Override
    public HttpAction restore(final WebContext context, final SessionStore sessionStore, final String defaultUrl) {
        final Optional<Object> optRequestedUrl = sessionStore.get(context, Pac4jConstants.REQUESTED_URL);
        HttpAction requestedAction = null;
        if (optRequestedUrl.isPresent()) {
            sessionStore.set(context, Pac4jConstants.REQUESTED_URL, "");
            final Object requestedUrl = optRequestedUrl.get();
            if (requestedUrl instanceof FoundAction) {
                requestedAction = (FoundAction) requestedUrl;
            } else if (requestedUrl instanceof OkAction) {
                requestedAction = (OkAction) requestedUrl;
            }
        }
        if (requestedAction == null) {
            requestedAction = new FoundAction(defaultUrl);
        }

        LOGGER.debug("requestedAction: {}", requestedAction.getMessage());
        if (requestedAction instanceof FoundAction) {
            return HttpActionHelper.buildRedirectUrlAction(context, ((FoundAction) requestedAction).getLocation());
        } else {
            return HttpActionHelper.buildFormPostContentAction(context, ((OkAction) requestedAction).getContent());
        }
    }
}
