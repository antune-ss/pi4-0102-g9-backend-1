package src.Cliente;

public class Requisicao {
    private String sql;

    private void validaString(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("Requisição inválida");
    }

    Requisicao(String sql) {
        setSql(sql);
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        validaString(sql);
        this.sql = sql;
    }
}
