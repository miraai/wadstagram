package wadstagram.controller;

import java.io.IOException;
import java.util.Date;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import wadstagram.domain.Account;
import wadstagram.domain.Comment;
import wadstagram.domain.Image;
import wadstagram.domain.ImageBytes;
import wadstagram.service.AccountService;
import wadstagram.service.ImageService;

@Controller
@RequestMapping("/image")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private AccountService accountService;

    @RequestMapping(value = "/{id}/delete", method = RequestMethod.POST)
    public String deleteImage(@PathVariable Long id) {
        this.imageService.deleteImage(id);
        return "redirect:/" + id;
    }

    @RequestMapping(value = "/{id}/comment", method = RequestMethod.POST)
    public String postComment(@PathVariable Long id, @RequestParam String comment) {
        Account sender = accountService.getUserByName(SecurityContextHolder.getContext().getAuthentication().getName());
        Image image = imageService.getImage(id);
        if (comment.isEmpty() || image == null) {
            return "redirect:/image/" + id;
        }
        Comment newComment = new Comment(image, sender, new Date(), comment);
        imageService.addCommentToPicture(image, newComment);
        return "redirect:/image/" + id;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public String getImage(@PathVariable Long id, Model model) {
        if (imageService.getImage(id) == null) {
            return "redirect:/";
        }
        Image image = imageService.getImage(id);
        if (image == null) {
            return "redirect:/image/" + id;
        }
        Account liker = accountService.getUserByName(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!image.getLikers().contains(liker)) {
            model.addAttribute("liked", false);
        } else {
            model.addAttribute("liked", true);
        }
        model.addAttribute("image", imageService.getImage(id));
        model.addAttribute("hearts", imageService.getHeartAmount(id));
        model.addAttribute("comments", imageService.getComments(id));
        return "image";
    }

    @Transactional
    @ResponseBody
    @RequestMapping(value = "{id}/data", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getImageData(@PathVariable Long id) {
        Image image = imageService.getImage(id);
        if (image == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(image.getFileSize());
        headers.setContentType(MediaType.parseMediaType(MediaType.IMAGE_PNG_VALUE));
        return new ResponseEntity<>(image.getImageData().get(), headers, HttpStatus.OK);
    }

    @Transactional
    @ResponseBody
    @RequestMapping(value = "{id}/thumbnaildata", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getThumbnailData(@PathVariable Long id) {
        Image image = imageService.getImage(id);
        if (image == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(image.getThumbnailSize());
        headers.setContentType(MediaType.parseMediaType(MediaType.IMAGE_PNG_VALUE));
        return new ResponseEntity<>(image.getThumbnailData().get(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "{id}/like", method = RequestMethod.POST)
    public String likeImage(@PathVariable Long id) {
        Image image = imageService.getImage(id);
        if (image == null) {
            return "redirect:/image/" + id;
        }
        Account liker = accountService.getUserByName(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!image.getLikers().contains(liker)) {
            image.getLikers().add(liker);
        } else {
            image.getLikers().remove(liker);
        }
        imageService.saveImage(image);
        return "redirect:/image/" + id;
    }

    @RequestMapping(method = RequestMethod.POST)
    public String addImage(@RequestParam("image") MultipartFile received, @RequestParam String description) throws IOException {
        if (!received.getContentType().contains("image")) {
            return "redirect:/?error";
        }
        if (description.isEmpty()) {
            return "redirect:/";
        }
        Image image = imageService.createImage(received, new Image(), new ImageBytes(), new ImageBytes(), description);
        return "redirect:/";
    }
}
