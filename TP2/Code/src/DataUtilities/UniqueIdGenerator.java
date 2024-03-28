package DataUtilities;

import java.util.UUID;

public class UniqueIdGenerator
{
    public static String generateUUID()
    {
        return UUID.randomUUID().toString();
    }
}
