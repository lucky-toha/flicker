package software.sigma.flicker.messageservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.sigma.flicker.messageservice.domain.TagRepository;
import software.sigma.flicker.messageservice.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tag RESTful controller.
 *
 * @author Anton Taranukha
 */
@Api(value = "Tags data operations ", description = "RESTful API to interact with tags resources.")
@RestController
@RequestMapping(value = "/api/v1/messages/tags")
public class TagRestController {

    private static final String DEFAULT_TAG_SIZE = "10";

    private static final String SORTED_FIELD = "used";

    @Autowired
    private TagRepository tagRepository;

    /**
     * Returns the specified number of tags sorted by the field 'used'.
     *
     * @param count count of tags
     * @return tags
     */
    @ApiOperation(value = "Get most popular tags", httpMethod = "GET", responseContainer = "List", response = String.class,
            notes = "Returns the specified number of the most popular tags.",
            authorizations = {@Authorization(value = "oauth2", scopes = { @AuthorizationScope(scope = "read", description = "read data")})})
    @RequestMapping(method = RequestMethod.GET)
    public List<String> getHottestTags(
            @ApiParam(value = "Number of tags.", required = false, defaultValue = DEFAULT_TAG_SIZE)
            @RequestParam(value = "count", required = false, defaultValue = DEFAULT_TAG_SIZE) Integer count) {

        Pageable pageRequest = new PageRequest(0, count, new Sort(Sort.Direction.DESC, SORTED_FIELD));
        List<Tag> tags = tagRepository.findAll(pageRequest).getContent();
        return tags.stream().map(Tag::getName).collect(Collectors.toList());
    }
}
