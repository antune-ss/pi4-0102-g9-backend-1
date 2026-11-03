package src.Servidor;

// **essa classe foi uma ideia, que pode ser removida caso não faça sentido
// vamos receber os SQLs prontos do frontend e apenas executá-los
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
