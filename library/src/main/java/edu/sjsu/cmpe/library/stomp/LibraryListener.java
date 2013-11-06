package edu.sjsu.cmpe.library.stomp;

import java.net.URL;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Message;

import org.eclipse.jetty.server.Server;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;


import com.yammer.dropwizard.lifecycle.ServerLifecycleListener;

import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

public class LibraryListener implements ServerLifecycleListener {
	
	    private  BookRepositoryInterface bookRepository;
	    
	    private String queueName;    
	    private String topicName;    
	    private String libraryName;
	    private String apolloUser;
	    private String apolloPassword;
	    private String apolloHost;
	    private String apolloPort;
	
	public LibraryListener(BookRepositoryInterface bookRepository, String queueName, String topicName,
			String libraryName, String apolloUser, String apolloPassword,
			String apolloHost, String apolloPort){
		
		this.bookRepository = bookRepository;
		
		this.queueName = queueName;
		this.topicName = topicName;
		this.libraryName = libraryName;
		this.apolloUser = apolloUser;
		this.apolloPassword = apolloPassword;
		this.apolloHost = apolloHost;
		this.apolloPort = apolloPort;		
		
	}

	@Override
	public void serverStarted(Server server) {
		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	    factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);
	    Connection connection;
		try {
			connection = factory.createConnection(apolloUser, apolloPassword);
		
	    connection.start();
	    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    Destination dest = new StompJmsDestination(topicName);
	    MessageConsumer consumer = session.createConsumer(dest);
	    
	    while (true) {
	        Message msg = (Message) consumer.receive();
	        System.out.println("Received Book------->"+ ((TextMessage)msg).getText());
	        
	        /**Parsing the message received form the Topic*/
	        String[] finalMessage=((TextMessage)msg).getText().toString().split(":", 4); 
	        Long isbn = Long.valueOf(finalMessage[0]);
	        Status status = Status.available;

	        Book book = bookRepository.getBookByISBN(isbn);
	        
	        /**If book received from Publisher is a lost book, update its status*/
	        if (book != null && book.getStatus()==Status.lost) {
	                book.setStatus(status);
	                System.out.println("Updated status for book with ISBN" + book.getIsbn() + "as AVAILABLE" );
	        }
	        
	        /**Adding new book received from the publisher to the hashmap*/
	        else if (book == null){
	                String title = finalMessage[1];
	                String category = finalMessage[2];
	                URL coverImage = new URL(finalMessage[3]);
	                book = new Book();
	                book.setIsbn(isbn);
	                book.setTitle(title);
	                book.setCategory(category);
	                book.setCoverimage(coverImage);
	                bookRepository.saveBook(book);
	                System.out.println("Adding new book with ISBN " + book.getIsbn() + " to the library");
	        }
	        
	        /** if book is already there in the library*/
	        else {
	                System.out.println("Book with ISBN " + book.getIsbn() + " already AVAILABLE in Library");
	        }

		
	    }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
    

	}
}