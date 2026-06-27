package Model;

import Server.IServerStrategy;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Adds request logging around an existing server strategy without changing its behavior.
 */
class LoggingServerStrategy implements IServerStrategy {
    private final IServerStrategy delegate;
    private final Logger logger;
    private final String operationName;

    LoggingServerStrategy(IServerStrategy delegate, Logger logger, String operationName) {
        this.delegate = delegate;
        this.logger = logger;
        this.operationName = operationName;
    }

    /** Logs request timing around the wrapped strategy call. */
    @Override
    public void serverStrategy(InputStream inputStream, OutputStream outputStream) {
        long startTime = System.currentTimeMillis();
        logger.info("{} request started.", operationName);

        try {
            delegate.serverStrategy(inputStream, outputStream);
            logger.info("{} request completed in {} ms.", operationName, System.currentTimeMillis() - startTime);
        } catch (RuntimeException e) {
            logger.error("{} request failed after {} ms.", operationName, System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }
}
