package ds.gae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.TransactionOptions;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;
import ds.gae.listener.CarRentalServletContextListener;
 
public class CarRentalModel {
	
	public Map<String,CarRentalCompany> CRCS = new HashMap<String, CarRentalCompany>();	
	
	private static CarRentalModel instance;
	
	
	public List<String> getCarRentalCompanyNames() {
		EntityManager em = EMF.get().createEntityManager();
		
		List<String> names = new ArrayList((List<String>)em.createNamedQuery("ds.gae.entities.CarRentalCompany.getCarRentalCompanyNames")
				.getResultList());
		em.close();
    	return names;
	}
	
    public Key storeCarType(CarType carType) {
    	EntityManager em = EMF.get().createEntityManager();
    	EntityTransaction tx = em.getTransaction();
        
        try {
            tx.begin();
            em.persist(carType);
            em.getTransaction().commit();
        } catch (Exception e) {
            Logger log = Logger.getLogger(CarRentalServletContextListener.class.getName());
            log.log(Level.WARNING, "Rolling Back:", e);
            tx.rollback();
        } finally {
            em.close();
        }
        
        return carType.getId();
    }
    

    public int createCar(int uid,  Key key) {
    	EntityManager em = EMF.get().createEntityManager();
    	
    	EntityTransaction tx = em.getTransaction();
        CarType carType = em.find(CarType.class, key);
        Car car = new Car(uid, carType);
    	
    	try {
            tx.begin();
            em.persist(car);
            em.getTransaction().commit();
        } catch (Exception e) {
            Logger log = Logger.getLogger(CarRentalServletContextListener.class.getName());
            log.log(Level.WARNING, "Rolling Back:", e);
            tx.rollback();
        } finally {
            em.close();
        }
    	
        return car.getId();
    }


    public void createCarRentalCompany(String name) {
    	EntityManager em = EMF.get().createEntityManager();
    	EntityTransaction tx = em.getTransaction();
    	
    	CarRentalCompany crc = new CarRentalCompany(name,new HashSet<Car>());
    	try {
            tx.begin();
            em.persist(crc);
            em.getTransaction().commit();
        } catch (Exception e) {
            Logger log = Logger.getLogger(CarRentalServletContextListener.class.getName());
            log.log(Level.WARNING, "Rolling Back:", e);
            tx.rollback();
        } finally {
            em.close();
        }
    	
    }
    
    public void createCarRentalCompany(String name, Set<Car> cars) {
    	EntityManager em = EMF.get().createEntityManager();
    	
    	EntityTransaction tx = em.getTransaction();
    	CarRentalCompany crc = new CarRentalCompany(name,cars);
    	try {
            tx.begin();
            em.persist(crc);
            tx.commit();
        } catch (Exception e) {
            Logger log = Logger.getLogger(CarRentalServletContextListener.class.getName());
            log.log(Level.WARNING, "Rolling Back:", e);
            tx.rollback();
        } finally {
            em.close();
        }
    	
    }

	
	
	public static CarRentalModel get() {
		if (instance == null)
			instance = new CarRentalModel();
		return instance;
	}
		
	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param 	crcName
	 * 			the car rental company
	 * @return	The list of car types (i.e. name of car type), available
	 * 			in the given car rental company.
	 */
	public Set<String> getCarTypesNames(String crcName) {
		EntityManager em = EMF.get().createEntityManager();
		
		Set<String> names = new HashSet<String>((Set<String>)em.createNamedQuery("ds.gae.entities.CarType.getCarTypesForCompany")
				.setParameter("company", crcName)
				.getResultList());
		em.close();
		
		return names;
	}

	
	
    /**
     * Get all registered car rental companies
     *
     * @return	the list of car rental companies
     */
    public Collection<String> getAllRentalCompanyNames() {
    	EntityManager em = EMF.get().createEntityManager();
		
		List<String> names = new ArrayList((List<String>)em.createNamedQuery("ds.gae.entities.CarRentalCompany.getCarRentalCompanyNames")
				.getResultList());
		em.close();
    	return names;
    }
	
	/**
	 * Create a quote according to the given reservation constraints (tentative reservation).
	 * 
	 * @param	company
	 * 			name of the car renter company
	 * @param	renterName 
	 * 			name of the car renter 
	 * @param 	constraints
	 * 			reservation constraints for the quote
	 * @return	The newly created quote.
	 *  
	 * @throws ReservationException
	 * 			No car available that fits the given constraints.
	 */
    public Quote createQuote(String companyName, String renterName, ReservationConstraints constraints) throws ReservationException {
    	EntityManager em = EMF.get().createEntityManager();
    	try {
            CarRentalCompany company = (CarRentalCompany) em.createNamedQuery("ds.gae.entities.CarRentalCompany.getCarRentalCompanyByName")
            		.setParameter("company", companyName)
            		.getResultList().get(0);
            
            Quote out = company.createQuote(constraints, renterName);
            System.out.println(out.toString());
            em.close();
            return out;
        } catch(Exception e) {
            throw new ReservationException(e.toString());
        }
    }
    
	/**
	 * Confirm the given quote.
	 *
	 * @param 	q
	 * 			Quote to confirm
	 * 
	 * @throws ReservationException
	 * 			Confirmation of given quote failed.	
	 */
	public void confirmQuote(Quote q) throws ReservationException {
		EntityManager em = EMF.get().createEntityManager();
		try{
			CarRentalCompany company = (CarRentalCompany) em.createNamedQuery("ds.gae.entities.CarRentalCompany.getCarRentalCompanyByName")
            		.setParameter("company", q.getRentalCompany())
            		.getResultList().get(0);
			Reservation res = company.confirmQuote(q);
			em.close();	

		}
		catch(Exception e){
			throw new ReservationException(e.toString());
		}
	}
	
    /**
	 * Confirm the given list of quotes
	 * 
	 * @param 	quotes 
	 * 			the quotes to confirm
	 * @return	The list of reservations, resulting from confirming all given quotes.
	 * 
	 * @throws 	ReservationException
	 * 			One of the quotes cannot be confirmed. 
	 * 			Therefore none of the given quotes is confirmed.
	 */
    public List<Reservation> confirmQuotes(List<Quote> quotes) throws ReservationException {    	
    	List<Reservation> reservations = new ArrayList<Reservation>();
    	
    	
    	
    	EntityManager em = EMF.get().createEntityManager();
    	
    	EntityTransaction t = em.getTransaction();
    	
	    try{
	        t.begin();
	        for(Quote q: quotes){
	        	
	        	
				CarRentalCompany company = (CarRentalCompany) em.createNamedQuery("ds.gae.entities.CarRentalCompany.getCarRentalCompanyByName")
	            		.setParameter("company", q.getRentalCompany())
	            		.getResultList().get(0);
				Reservation res = company.confirmQuote(q);
	        	
	        	reservations.add(res);
	        }
	        t.commit();
	    }catch(Exception e) {
	        if (t.isActive()){
	            t.rollback();
	            //reservations = new ArrayList<Reservation>();
	        }
	        throw new ReservationException(e.toString());
	    }
	    finally{
	    	em.close();
	    }
	    return reservations;
    }
	
	/**
	 * Get all reservations made by the given car renter.
	 *
	 * @param 	renter
	 * 			name of the car renter
	 * @return	the list of reservations of the given car renter
	 */
	public List<Reservation> getReservations(String renter) {
		EntityManager em = EMF.get().createEntityManager();
		

		List<Reservation> reservations = new ArrayList<Reservation>((List<Reservation>)em.createNamedQuery("ds.gae.entities.CarRentalCompany.getReservationsOfRenter")
				.setParameter("renter", renter).getResultList());
		em.close();
    	return reservations;
    }

    /**
     * Get the car types available in the given car rental company.
     *
     * @param 	crcName
     * 			the given car rental company
     * @return	The list of car types in the given car rental company.
     */
    public Collection<CarType> getCarTypesOfCarRentalCompany(String crcName) {
    	EntityManager em = EMF.get().createEntityManager();
		
    	HashMap<String,CarType> carTypes = new HashMap<String,CarType>((Map<String,CarType>)em.createNamedQuery("ds.gae.entities.CarRentalCompany.getCarTypesForCompany")
				.setParameter("company", crcName)
				.getResultList().get(0));
		em.close();
		
    	return carTypes.values();
    }
	
    /**
     * Get the list of cars of the given car type in the given car rental company.
     *
     * @param	crcName
	 * 			name of the car rental company
     * @param 	carType
     * 			the given car type
     * @return	A list of car IDs of cars with the given car type.
     */
    public Collection<Integer> getCarIdsByCarType(String crcName, CarType carType) {
    	Collection<Integer> out = new ArrayList<Integer>();
    	for (Car c : getCarsByCarType(crcName, carType)) {
    		out.add(c.getId());
    	}
    	return out;
    }
    
    /**
     * Get the amount of cars of the given car type in the given car rental company.
     *
     * @param	crcName
	 * 			name of the car rental company
     * @param 	carType
     * 			the given car type
     * @return	A number, representing the amount of cars of the given car type.
     */
    public int getAmountOfCarsByCarType(String crcName, CarType carType) {
    	return this.getCarsByCarType(crcName, carType).size();
    }

	/**
	 * Get the list of cars of the given car type in the given car rental company.
	 *
	 * @param	crcName
	 * 			name of the car rental company
	 * @param 	carType
	 * 			the given car type
	 * @return	List of cars of the given car type
	 */
	private Collection<Car> getCarsByCarType(String crcName, CarType carType) {				
		EntityManager em = EMF.get().createEntityManager();
		
		/*
		 * Adding a field to carType containing the crc will result in 1 query.
		 */
		
		HashSet<Car> cars = new HashSet((Set<Car>)em.createNamedQuery("ds.gae.entities.CarRentalCompany.getCarsByCarTypeAndCompanyName")
				.setParameter("company", crcName)
				.getResultList().get(0));
		em.close();
		ArrayList<Car> result = new ArrayList<Car>();
		for(Car c : cars) {
			if(c.getType().equals(carType)) {
				result.add(c);
			}
			
		}
    	return result;
	}

	/**
	 * Check whether the given car renter has reservations.
	 *
	 * @param 	renter
	 * 			the car renter
	 * @return	True if the number of reservations of the given car renter is higher than 0.
	 * 			False otherwise.
	 */
	public boolean hasReservations(String renter) {
		return this.getReservations(renter).size() > 0;		
	}	
}