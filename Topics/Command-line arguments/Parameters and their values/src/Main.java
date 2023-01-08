
class Problem {
    public static void main(String[] args) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (i % 2 == 0) {
                map.put(args[i], args[i + 1]);
            }
        }
        //Print out map as key=value
        map.forEach((key, value) -> System.out.println(key + "=" + value));
    }
}