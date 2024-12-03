package com.parkit.parkingsystem.service;

import java.time.Duration;
import java.time.ZoneId;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public void calculateFare(Ticket ticket) {

		// Vérification des données
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect: " + ticket.getOutTime());
		}

// Calcul précis de la durée en minutes
		long durationInMinutes = Duration
				.between(ticket.getInTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
						ticket.getOutTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
				.toMinutes();

		// Gratuité pour les 30 premières minutes
		if (durationInMinutes <= 30) {
			ticket.setPrice(0);
			return; // Terminer la méthode
		}

// Convertir la durée en heures avec des fractions
		double durationInHours = durationInMinutes / 60.0;

		// Débogage temporaire
		System.out.println("Duration in minutes: " + durationInMinutes);
		System.out.println("Duration in hours: " + durationInHours);
		System.out.println("Parking Type: " + ticket.getParkingSpot().getParkingType());

// Calcul du tarif en fonction du type de véhicule
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
	}
}
