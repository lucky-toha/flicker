package software.sigma.flicker.util;

import org.junit.Test;
import software.sigma.flicker.messageservice.util.MessageUtils;

import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test class for the {@link MessageUtils} class.
 *
 * @author Anton Taranukha
 */
public class MessageUtilsTest {

    private static final String TEXT = " text_part ";

    private static final String FIRST_TAG = "first_tag";

    private static final String SECOND_TAG = "second_tag";

    @Test
    public void shouldExtractTags() {
        String text = TEXT + MessageUtils.HASH_TAG +FIRST_TAG
                + TEXT + MessageUtils.HASH_TAG +SECOND_TAG
                + TEXT + MessageUtils.HASH_TAG +SECOND_TAG;

        Set<String> tagNames = MessageUtils.extractTags(text);
        assertEquals(2, tagNames.size());
        assertTrue(tagNames.contains(FIRST_TAG));
        assertTrue(tagNames.contains(SECOND_TAG));
    }

    @Test
    public void shouldIgnoreEmptyTags() {
        String text = TEXT + MessageUtils.HASH_TAG +FIRST_TAG
                + TEXT + MessageUtils.HASH_TAG +"";

        Set<String> tagNames = MessageUtils.extractTags(text);
        assertEquals(1, tagNames.size());
        assertTrue(tagNames.contains(FIRST_TAG));
    }
}
