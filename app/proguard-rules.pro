# Strip verbose and debug logging from release builds, including the message lambdas.
-assumenosideeffects class io.github.gobinathm.portalistv.core.common.log.Log {
    public void v(...);
    public void d(...);
}
