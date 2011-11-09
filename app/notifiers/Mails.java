package notifiers;

import models.blog.Comment;
import models.blog.Post;
import play.mvc.Mailer;

public class Mails extends Mailer {

    public static void commentReplied(Comment comment, Post post) {
        setSubject("Your comment has been replied");
        addRecipient(comment.email);
        setFrom("goodgoodstudy.blog@gmail.com");
        setReplyTo("goodgoodstudy.blog@gmail.com");
        send(comment, post);
    }

}
