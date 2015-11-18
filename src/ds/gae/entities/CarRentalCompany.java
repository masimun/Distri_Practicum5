package ds.gae.entities;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.persistence.NamedQueries;

import com.google.appengine.api.datastore.Key;

import static javax.persistence.FetchType.LAZY;
import ds.gae.ReservationException;

@NamedQueries({	
    /*@NamedQuery(name="ds.gae.entities.CarRentalCompany.getNumberOfReservationsForCarTypeInCompany",
                query="SELECT COUNT(res) FROM Reservation res WHERE res.rentalCompany = LOWER(:companyName) AND res.carType = :carType"),
                */
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getReservationsOfRenter",
                query="SELECT res FROM Reservation res WHERE res.carRenter = :renter"),  
    
    /*@NamedQuery(name="ds.gae.entities.CarRentalCompany.getClientReservationList",
                query="SELECT res.carRenter,COUNT(res.carRenter) FROM Reservation res GROUP BY res.carRenter ORDER BY COUNT(res.carRenter) DESC"),
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getMostPopularCarTypeByCompany",
                query="SELECT ct FROM Reservation res JOIN CarType ct WHERE res.rentalCompany = LOWER(:company) GROUP BY ct ORDER BY COUNT(res.rentalCompany) DESC"),
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getCarIds",                           
                query="SELECT c.id FROM CarRentalCompany comp JOIN comp.cars c WHERE comp.name = :company AND c.type.name = :type"),
    */
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getCarRentalCompanyNames",                           
                query="SELECT comp.name FROM CarRentalCompany comp"),
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getCarRentalCompanyByName",                           
                query="SELECT comp FROM CarRentalCompany comp WHERE comp.name = :company"),
	/*
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getAllCarRentalCompanies",                           
                query="SELECT comp FROM CarRentalCompany comp"),*/
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getCarTypesForCompany",
    			//query="SELECT r.carTypes FROM CarRentalCompany r WHERE r.name = :company"),
                query="SELECT r.carTypes FROM CarRentalCompany r WHERE r.name = :company"),
    //@NamedQuery(name="ds.gae.entities.CarRentalCompany.getCarTypeNamesForCompany",
    //            query="SELECT t.name FROM CarRentalCompany r, IN (r.carTypes) t WHERE r.name = :company"),
                
                /*
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getCheapestCarType",
                query="SELECT ct FROM Car c JOIN c.type ct JOIN c.reservations res WHERE :end <= res.startDate OR res.endDate <= :start ORDER BY ct.rentalPricePerDay ")*/
    @NamedQuery(name="ds.gae.entities.CarRentalCompany.getCarRentalCompanyByName",
    			query="SELECT c FROM CarRentalCompany c WHERE c.name = :company")
    

	}) 

@Entity
public class CarRentalCompany {
	
	@Transient
	private static Logger logger = Logger.getLogger(CarRentalCompany.class.getName());
	@Id
	private String name;
	@OneToMany(cascade = {CascadeType.REMOVE,CascadeType.PERSIST}, fetch = LAZY)
	private Set<Car> cars;
	@ManyToMany(fetch = LAZY)
	private Map<String,CarType> carTypes = new HashMap<String, CarType>();

	/***************
	 * CONSTRUCTOR *
	 ***************/

	public CarRentalCompany(String name, Set<Car> cars) {
		logger.log(Level.INFO, "<{0}> Car Rental Company {0} starting up...", name);
		setName(name);
		this.cars = cars;
		for(Car car:cars)
			carTypes.put(car.getType().getName(), car.getType());
	}

	/********
	 * NAME *
	 ********/

	public String getName() {
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	/*************
	 * CAR TYPES *
	 *************/

	public Collection<CarType> getAllCarTypes() {
		return carTypes.values();
	}
	
	public CarType getCarType(String carTypeName) {
		if(carTypes.containsKey(carTypeName))
			return carTypes.get(carTypeName);
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}
	
	public boolean isAvailable(String carTypeName, Date start, Date end) {
		logger.log(Level.INFO, "<{0}> Checking availability for car type {1}", new Object[]{name, carTypeName});
		if(carTypes.containsKey(carTypeName))
			return getAvailableCarTypes(start, end).contains(carTypes.get(carTypeName));
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}
	
	public Set<CarType> getAvailableCarTypes(Date start, Date end) {
		Set<CarType> availableCarTypes = new HashSet<CarType>();
		for (Car car : cars) {
			if (car.isAvailable(start, end)) {
				availableCarTypes.add(car.getType());
			}
		}
		return availableCarTypes;
	}
	
	/*********
	 * CARS *
	 *********/
	
    public void addCar(Car car) {
        cars.add(car);
        carTypes.put(car.getType().getName(),car.getType());
    }
	
	private Car getCar(int uid) {
		for (Car car : cars) {
			if (car.getId() == uid)
				return car;
		}
		throw new IllegalArgumentException("<" + name + "> No car with uid " + uid);
	}
	
	public Set<Car> getCars() {
    	return cars;
    }
	
	private List<Car> getAvailableCars(String carType, Date start, Date end) {
		List<Car> availableCars = new LinkedList<Car>();
		for (Car car : cars) {
			if (car.getType().getName().equals(carType) && car.isAvailable(start, end)) {
				availableCars.add(car);
			}
		}
		return availableCars;
	}

	/****************
	 * RESERVATIONS *
	 ****************/

	public Quote createQuote(ReservationConstraints constraints, String client)
			throws ReservationException {
		logger.log(Level.INFO, "<{0}> Creating tentative reservation for {1} with constraints {2}", 
                        new Object[]{name, client, constraints.toString()});
		
		CarType type = getCarType(constraints.getCarType());
		
		if(!isAvailable(constraints.getCarType(), constraints.getStartDate(), constraints.getEndDate()))
			throw new ReservationException("<" + name
				+ "> No cars available to satisfy the given constraints.");
		
		double price = calculateRentalPrice(type.getRentalPricePerDay(),constraints.getStartDate(), constraints.getEndDate());
		
		return new Quote(client, constraints.getStartDate(), constraints.getEndDate(), getName(), constraints.getCarType(), price);
	}

	// Implementation can be subject to different pricing strategies
	private double calculateRentalPrice(double rentalPricePerDay, Date start, Date end) {
		return rentalPricePerDay * Math.ceil((end.getTime() - start.getTime())
						/ (1000 * 60 * 60 * 24D));
	}

	public Reservation confirmQuote(Quote quote) throws ReservationException {
		logger.log(Level.INFO, "<{0}> Reservation of {1}", new Object[]{name, quote.toString()});
		List<Car> availableCars = getAvailableCars(quote.getCarType(), quote.getStartDate(), quote.getEndDate());
		if(availableCars.isEmpty())
			throw new ReservationException("Reservation failed, all cars of type " + quote.getCarType()
	                + " are unavailable from " + quote.getStartDate() + " to " + quote.getEndDate());
		Car car = availableCars.get((int)(Math.random()*availableCars.size()));
		
		Reservation res = new Reservation(quote, car.getId());
		car.addReservation(res);
		return res;
	}

	public void cancelReservation(Reservation res) {
		logger.log(Level.INFO, "<{0}> Cancelling reservation {1}", new Object[]{name, res.toString()});
		getCar(res.getCarId()).removeReservation(res);
	}


}