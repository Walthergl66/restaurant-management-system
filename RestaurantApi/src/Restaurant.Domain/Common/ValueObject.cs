namespace Restaurant.Domain.Common;

public abstract class ValueObject : IEquatable<ValueObject>
{
    protected abstract IEnumerable<object?> GetEqualityComponents();

    public bool Equals(ValueObject? other) => other is not null && Equals(other);

    public override bool Equals(object? obj) => obj is ValueObject other && Equals(other);

    private bool Equals(ValueObject other) => GetEqualityComponents().SequenceEqual(other.GetEqualityComponents());

    public override int GetHashCode() => GetEqualityComponents()
        .Aggregate(17, (hash, component) => hash * 31 + (component?.GetHashCode() ?? 0));

    public static bool operator ==(ValueObject? left, ValueObject? right) => Equals(left, right);

    public static bool operator !=(ValueObject? left, ValueObject? right) => !Equals(left, right);
}