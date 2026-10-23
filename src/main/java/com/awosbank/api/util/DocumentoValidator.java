package com.awosbank.api.util;

public class DocumentoValidator {

    public static boolean isValid(String doc) {
        if (doc == null) return false;
        String clean = doc.replaceAll("\\D", "");
        if (clean.length() == 11) return isCpf(clean);
        if (clean.length() == 14) return isCnpj(clean);
        return false;
    }

    private static boolean isCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;
        int[] d = cpf.chars().map(c -> c - '0').toArray();
        int soma = 0;
        for (int i = 0; i < 9; i++) soma += d[i] * (10 - i);
        int r = (soma * 10) % 11;
        if (r == 10) r = 0;
        if (r != d[9]) return false;
        soma = 0;
        for (int i = 0; i < 10; i++) soma += d[i] * (11 - i);
        r = (soma * 10) % 11;
        if (r == 10) r = 0;
        return r == d[10];
    }

    private static boolean isCnpj(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;
        int[] d = cnpj.chars().map(c -> c - '0').toArray();
        int[] p1 = {5,4,3,2,9,8,7,6,5,4,3,2};
        int[] p2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
        int soma = 0;
        for (int i = 0; i < 12; i++) soma += d[i] * p1[i];
        int r = soma % 11;
        int dv1 = r < 2 ? 0 : 11 - r;
        if (dv1 != d[12]) return false;
        soma = 0;
        for (int i = 0; i < 13; i++) soma += d[i] * p2[i];
        r = soma % 11;
        int dv2 = r < 2 ? 0 : 11 - r;
        return dv2 == d[13];
    }
}