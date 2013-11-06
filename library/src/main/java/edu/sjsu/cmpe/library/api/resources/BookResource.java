package edu.sjsu.cmpe.library.api.resources;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;

import com.yammer.dropwizard.jersey.params.LongParam;
import com.yammer.metrics.annotation.Timed;

import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.dto.BookDto;
import edu.sjsu.cmpe.library.dto.BooksDto;
import edu.sjsu.cmpe.library.dto.LinkDto;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

@Path("/v1/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {
    /** bookRepository instance */
    private final BookRepositoryInterface bookRepository;
    
    private String queueName;    
    private String topicName;    
    private String libraryName;
    private String apolloUser;
    private String apolloPassword;
    private String apolloHost;
    private String apolloPort;
    

    /**
     * BookResource constructor
     * 
     * @param bookRepository
     *            a BookRepository instance
     */
    /////////////////////////////// 1.1
    public BookResource(BookRepositoryInterface bookRepository, String queueName, String topicName,
    					String libraryName, String apolloUser, String apolloPassword,
    					String apolloHost, String apolloPort) {
    /////////////////////////////// 1.1 	
	this.bookRepository = bookRepository;
	
	this.queueName = queueName;
	this.topicName = topicName;
	this.libraryName = libraryName;
	this.apolloUser = apolloUser;
	this.apolloPassword = apolloPassword;
	this.apolloHost = apolloHost;
	this.apolloPort = apolloPort;
	
    }

    @GET
    @Path("/{isbn}")
    @Timed(name = "view-book")
    public BookDto getBookByIsbn(@PathParam("isbn") LongParam isbn) {
	Book book = bookRepository.getBookByISBN(isbn.get());
	BookDto bookResponse = new BookDto(book);
	bookResponse.addLink(new LinkDto("view-book", "/books/" + book.getIsbn(),
		"GET"));
	bookResponse.addLink(new LinkDto("update-book-status", "/books/"
		+ book.getIsbn(), "PUT"));
	// add more links

	return bookResponse;
    }

    @POST
    @Timed(name = "create-book")
    public Response createBook(@Valid Book request) {
	
    // Stores new book in the BookRepository to retrieve it later.
    	
	Book savedBook = bookRepository.saveBook(request);

	String location = "/books/" + savedBook.getIsbn();
	BookDto bookResponse = new BookDto(savedBook);
	bookResponse.addLink(new LinkDto("view-book", location, "GET"));
	bookResponse
	.addLink(new LinkDto("update-book-status", location, "PUT"));

	return Response.status(201).entity(bookResponse).build();
    }

    @GET
    @Path("/")
    @Timed(name = "view-all-books")
    public BooksDto getAllBooks() {
	BooksDto booksResponse = new BooksDto(bookRepository.getAllBooks());
	booksResponse.addLink(new LinkDto("create-book", "/books", "POST"));

	return booksResponse;
    }

    @PUT
    @Path("/{isbn}")
    @Timed(name = "update-book-status")
    public Response updateBookStatus(@PathParam("isbn") LongParam isbn,
	    @DefaultValue("available") @QueryParam("status") Status status) {
    	
    	
    	/////////////////////////// changes for 1.1
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
        factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);
        try {
			Connection connection = factory.createConnection(apolloUser, apolloPassword);
		
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination dest = new StompJmsDestination(queueName);
        MessageProducer producer = session.createProducer(dest);
        TextMessage msg = session.createTextMessage(libraryName+":" + isbn);
        msg.setLongProperty("id", System.currentTimeMillis());
        producer.send(msg);
        } catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
    /////////////////////////// changes for 1.1
        
    Book book = bookRepository.getBookByISBN(isbn.get());
	book.setStatus(status);

	BookDto bookResponse = new BookDto(book);
	String location = "/books/" + book.getIsbn();
	bookResponse.addLink(new LinkDto("view-book", location, "GET"));

	return Response.status(200).entity(bookResponse).build();
    }

    @DELETE
    @Path("/{isbn}")
    @Timed(name = "delete-book")
    public BookDto deleteBook(@PathParam("isbn") LongParam isbn) {
	bookRepository.delete(isbn.get());
	BookDto bookResponse = new BookDto(null);
	bookResponse.addLink(new LinkDto("create-book", "/books", "POST"));

	return bookResponse;
    }
}

