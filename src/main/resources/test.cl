fun fib(n:int): int{
    if (n < 2) {
        return n;
    }
    return fib(n - 1) + fib(n - 2);
}
var x = fib(10);
printInt(fib(10));

