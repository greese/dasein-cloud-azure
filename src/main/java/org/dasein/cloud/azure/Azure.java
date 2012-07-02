package org.dasein.cloud.azure;

import org.apache.log4j.Logger;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.compute.AzureComputeServices;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Random;

/**
 * Core cloud provider implementation for the Microsoft Azure cloud.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class Azure extends AbstractCloud {
    static private @Nonnull String getLastItem(@Nonnull String name) {
        int idx = name.lastIndexOf('.');

        if( idx < 0 ) {
            return name;
        }
        else if( idx == (name.length()-1) ) {
            return "";
        }
        return name.substring(idx+1);
    }

    static public @Nonnull Logger getLogger(@Nonnull Class<?> cls, @Nonnull String type) {
        String pkg = getLastItem(cls.getPackage().getName());

        if( pkg.equals("os") ) {
            pkg = "";
        }
        else {
            pkg = pkg + ".";
        }
        return Logger.getLogger("dasein.cloud.azure." + type + "." + pkg + getLastItem(cls.getName()));
    }

    public Azure() { }

    static private final Random random = new Random();
    
    public @Nonnull String generateToken(int minLength, int maxLength) {
        if( minLength < 0 ) {
            minLength = 0;
        }
        if( maxLength < minLength ) {
            if( minLength == 0 ) {
                return "";
            }
            maxLength = minLength;
        }
        int length = ((maxLength == minLength) ? minLength : random.nextInt(maxLength-minLength) + minLength);
        StringBuilder token = new StringBuilder();
        
        while( token.length() < length ) {
            char c = (char)random.nextInt(256);
            
            if( (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >='A' && c <= 'Z') || c == '!' || c == '@' || c == '^' || c == '=' || c == '+' || c == ',' ) {
                token.append(c);
            }
        }
        return token.toString();
    }
    
    @Override
    public @Nonnull String getCloudName() {
        return "Azure";
    }

    @Override
    public @Nonnull AzureComputeServices getComputeServices() {
        return new AzureComputeServices(this);
    }

    @Override
    public @Nonnull AzureLocation getDataCenterServices() {
        return new AzureLocation(this);
    }
    
    @Override
    public @Nonnull String getProviderName() {
        return "Microsoft";
    }

    public long parseTimestamp(@Nullable String time) throws CloudException {
        if( time == null ) {
            return 0L;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if( time.length() > 0 ) {
            try {
                return fmt.parse(time).getTime();
            }
            catch( ParseException e ) {
                fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                try {
                    return fmt.parse(time).getTime();
                }
                catch( ParseException encore ) {
                    throw new CloudException("Could not parse date: " + time);
                }
            }
        }
        return 0L;
    }

    @Override
    public @Nullable String testContext() {
        Logger logger = getLogger(Azure.class, "std");
        
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + Azure.class.getName() + ".testContext()");
        }
        try {
            try {
                AzureMethod method = new AzureMethod(this);
                ProviderContext ctx = getContext();
                
                if( ctx == null ) {
                    logger.error("No context was specified for a context test");
                    return null;
                }
                if( method.getAsStream(ctx.getAccountNumber(), "/locations") == null ) {
                    logger.warn("Account number was invalid for context test: " + ctx.getAccountNumber());
                    return null;
                }
                if( logger.isDebugEnabled() ) {
                    logger.debug("Valid account: " + ctx.getAccountNumber());
                }
                return ctx.getAccountNumber();
            }
            catch( Exception e ) {
                logger.error("Failed to test context: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + Azure.class.getName() + ".testContext()");
            }
        }
    }
}
