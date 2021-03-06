package software.sigma.flicker.messageservice.service;

import java.util.Set;

/**
 * Tag service-helper.
 *
 * @author Anton Taranukha
 */
public interface TagService {

    /**
     * Adds tags if they are not present in DB. Otherwise, increases value of the field 'used' in the tag.
     *
     * @param tagNames tag names
     */
    void addTags(Set<String> tagNames);

}
