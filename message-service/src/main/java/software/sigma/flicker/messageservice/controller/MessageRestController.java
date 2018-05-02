package software.sigma.flicker.messageservice.controller;

import io.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import software.sigma.flicker.error.ApiError;
import software.sigma.flicker.exception.RequestException;
import software.sigma.flicker.messageservice.domain.MessageRepository;
import software.sigma.flicker.messageservice.errors.ResponseExceptionHandler;
import software.sigma.flicker.messageservice.model.Message;
import software.sigma.flicker.messageservice.model.MessageDTO;
import software.sigma.flicker.messageservice.model.MessageResponse;
import software.sigma.flicker.messageservice.model.User;
import software.sigma.flicker.messageservice.service.TagService;
import software.sigma.flicker.messageservice.service.UserFeignService;
import software.sigma.flicker.messageservice.util.MessageUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Message RESTful controller.
 *
 * @author Anton Taranukha
 */
@Api(value = "Messages data operations ", description = "RESTful API to interact with messages resources.")
@RestController
@RequestMapping(value = "/api/v1/messages/items")
public class MessageRestController {

    private static final String AUTH_TOKEN = "Authorization";

    private static final String MESSAGE_NOT_FOUND = "Message not found";

    private static final String DELETING_FORBIDDEN = "Deleting another user's message is forbidden";

    private static final String DEFAULT_PAGE_NUMBER = "0";

    private static final String DEFAULT_PAGE_SIZE = "5";

    private static final String SORTED_FIELD = "createdAt";

    @Autowired
    private UserFeignService userFeignService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TagService tagService;

    /**
     * Returns messages according to the search parameters.
     *
     * @param jwtToken  jwtToken
     * @param tag        tag
     * @param pageNumber page number
     * @param pageSize   page size
     * @return response with entities
     */
    @PreAuthorize("#oauth2.hasScope('read')")
    @ApiOperation(value = "Get messages", response = MessageResponse.class,
            notes = "Get messages using parameters.",
            authorizations = {@Authorization(value = "oauth2", scopes = {@AuthorizationScope(scope = "read", description = "read data")})})
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<MessageResponse> getMessages(@RequestHeader(AUTH_TOKEN) String jwtToken,
                                                       @ApiParam(value = "The search string that used to find messages by hash tag.", required = false, defaultValue = "")
                                                       @RequestParam(value = "tag", required = false) String tag,
                                                       @ApiParam(value = "The page number for pagination.", required = false, defaultValue = DEFAULT_PAGE_NUMBER)
                                                       @RequestParam(value = "pageNumber", required = false, defaultValue = DEFAULT_PAGE_NUMBER) Integer pageNumber,
                                                       @ApiParam(value = "The page size for pagination.", required = false, defaultValue = DEFAULT_PAGE_SIZE)
                                                       @RequestParam(value = "pageSize", required = false, defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize) {

        Page<Message> pageMessages;
        Pageable pageRequest = new PageRequest(pageNumber, pageSize, new Sort(Sort.Direction.DESC, SORTED_FIELD));

        if (StringUtils.isBlank(tag)) {
            pageMessages = messageRepository.findAll(pageRequest);
        } else {
            pageMessages = messageRepository.findByTagNamesContaining(pageRequest, tag);
        }

        List<MessageDTO> messageDTOs = new ArrayList<>();


        for (Message message : pageMessages.getContent()) {
            User user = userFeignService.getByUsername(jwtToken, message.getUsername());
            if (user != null) {
                messageDTOs.add(new MessageDTO(message.getId(), message.getText(), user.getUsername(), user.getFirstName(), user.getLastName(),
                        message.getCreatedAt()));
            }
        }

        MessageResponse messageResponse = new MessageResponse(messageDTOs, pageMessages.isFirst(), pageMessages.isLast(), pageMessages.getNumberOfElements(),
                pageMessages.getTotalPages(), pageMessages.getTotalElements(), pageMessages.getSize(), pageMessages.getNumber());

        return ResponseEntity.ok().body(messageResponse);
    }

    /**
     * Creates and saves message with certain text.
     * Extracts tags from the text and saves them as Tag entities.
     *
     * @param principal principal
     * @param text      text
     * @return saved message
     */
    @Validated
    @ApiOperation(value = "Save message", httpMethod = "POST", response = MessageDTO.class,
            notes = "Saves message.",
            authorizations = {@Authorization(value = "oauth2", scopes = {@AuthorizationScope(scope = "write", description = "write data")})})
    @ApiResponses(value = {@ApiResponse(code = 400, message = ResponseExceptionHandler.MESSAGE_VALIDATION_ERROR, response = ApiError.class)})
    @RequestMapping(method = RequestMethod.POST, consumes = "text/plain")
    public ResponseEntity<MessageDTO> saveMessage(@RequestHeader(AUTH_TOKEN) String authToken, Principal principal, @RequestBody String text) {
        Set<String> tagNames = MessageUtils.extractTags(text);
        tagService.addTags(tagNames);
        Message message = messageRepository.save(new Message(text, principal.getName(), LocalDateTime.now(), tagNames));

        User user = userFeignService.getByUsername(authToken, principal.getName());
        MessageDTO messageDTO = new MessageDTO(message.getId(), message.getText(), user.getUsername(), user.getFirstName(), user.getLastName(),
                message.getCreatedAt());

        return ResponseEntity.ok().body(messageDTO);
    }

    /**
     * Deletes message with certain id.
     *
     * @param principal principal
     * @param id        id
     * @return response with the operation result
     */
    @ApiOperation(value = "Delete message", httpMethod = "DELETE", response = String.class,
            notes = "Deletes message with specified id.",
            authorizations = {@Authorization(value = "oauth2", scopes = {@AuthorizationScope(scope = "write", description = "write data")})})
    @ApiResponses(value = {@ApiResponse(code = 404, message = MESSAGE_NOT_FOUND, response = ApiError.class),
            @ApiResponse(code = 406, message = DELETING_FORBIDDEN, response = ApiError.class)})
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity deleteMessage(Principal principal,
                                        @ApiParam(value = "Message id to delete.", required = true) @RequestParam(value = "id", required = true) String id) {
        Message message = messageRepository.findOne(id);

        if (message == null) {
            throw new RequestException(HttpStatus.NOT_FOUND, MESSAGE_NOT_FOUND);
        }

        if (!message.getUsername().equals(principal.getName())) {
            throw new RequestException(HttpStatus.NOT_ACCEPTABLE, DELETING_FORBIDDEN);
        }

        messageRepository.delete(id);

        return ResponseEntity.ok().body(null);
    }

}
