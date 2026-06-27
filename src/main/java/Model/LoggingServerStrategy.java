package Model;

import Server.IServerStrategy;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Decorates a Part B server strategy with Log4j2 logging.
 *
 * The real generation/solving behavior still belongs to the strategy from ATPProjectJAR.
 * This wrapper only records when a server starts and finishes handling a client request.
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

    /**
     * Called by the JAR Server for every accepted client connection.
     * Timing is measured around the wrapped strategy so each server log includes request duration.
     */
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
