namespace Restaurant.Domain.Common;

public class DomainException(string message) : Exception(message);