package com.parkit.parkingsystem.service;

import java.time.Duration;
import java.time.ZoneId;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	// Method without the discount parameter
	public void calculateFare(Ticket ticket) {
	    calculateFare(ticket, false); // Calls the overloaded method with "discount" set to false
	}

		public void calculateFare(Ticket ticket, boolean discount) {

			// Data validation
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect: " + ticket.getOutTime());
		}

		// Precise calculation of duration in minutes
		long durationInMinutes = Duration
				.between(ticket.getInTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
						ticket.getOutTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
				.toMinutes();

		// Free parking for the first 30 minutes
		if (durationInMinutes <= 30) {
			ticket.setPrice(0);
			return; // End of the method
		}

		// Convert the duration to hours with fractions
		double durationInHours = durationInMinutes / 60.0;

		// Temporary debugging
		System.out.println("Duration in minutes: " + durationInMinutes);
		System.out.println("Duration in hours: " + durationInHours);
		System.out.println("Parking Type: " + ticket.getParkingSpot().getParkingType());

		// Calculate the fare based on the vehicle type
		switch (ticket.getParkingSpot().getParkingType()) {
		case CAR:
			ticket.setPrice(durationInHours * Fare.CAR_RATE_PER_HOUR);
			break;
		case BIKE:
			ticket.setPrice(durationInHours * Fare.BIKE_RATE_PER_HOUR);
			break;
		default:
			throw new IllegalArgumentException("Unknown Parking Type");
		}
		
		// Apply the discount if the ticket has a discount
	    if (discount) {
	        ticket.setPrice(ticket.getPrice() * 0.95);
	    }
	}

	}

