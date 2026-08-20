package com.transit.platform.common.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Helper commun de mise en page PDF (Prompt 01 §18, Prompt 04 §61).
 *
 * Réécriture (demande utilisateur explicite) : "les données de la facture sont
 * standardisées, mais sa présentation est personnalisable par chaque entreprise". Trois
 * modèles visuels au choix (MODERNE / CLASSIQUE / MINIMALISTE), sélectionné par entreprise
 * via Paramètres, plus une couleur d'accent personnalisable qui s'applique dans les trois.
 * Les DONNÉES transmises à genererDocument restent strictement identiques quel que soit le
 * modèle choisi — seul le rendu change.
 *
 * Chaque modèle a été prototypé et validé visuellement (rendu Python/reportlab) avant sa
 * traduction ici. Dessin par coordonnées absolues (PdfContentByte), pas de flux
 * Paragraph/PdfPTable — nécessaire pour la découpe en diagonale du modèle MODERNE.
 *
 * Limite connue et assumée : au-delà d'une vingtaine de lignes, le tableau continue sur une
 * nouvelle page avec un en-tête de tableau simplifié (pas de re-dessin de la bande
 * graphique complète) — cas rare en pratique pour une facture de transit.
 */
public final class PdfDocumentBuilder {

    public enum Template { MODERNE, CLASSIQUE, MINIMALISTE }

    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color ROW_ALT = new Color(248, 250, 252);
    private static final Color SUCCESS = new Color(21, 128, 61);
    private static final Color DANGER = new Color(185, 28, 28);
    private static final Color WHITE = Color.WHITE;
    private static final Color DEFAULT_ACCENT = new Color(30, 58, 95);

    private static final float MARGIN = 40;
    private static final float PAGE_W = PageSize.A4.getWidth();
    private static final float PAGE_H = PageSize.A4.getHeight();

    private static BaseFont FONT_NORMAL;
    private static BaseFont FONT_BOLD;
    private static BaseFont FONT_ITALIC;

    static {
        try {
            FONT_NORMAL = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            FONT_BOLD = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            FONT_ITALIC = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger les polices PDF de base", e);
        }
    }

    public record EnTeteEntreprise(
            String nom, String adresse, String telephone, String email, String siteWeb,
            String rccm, String ifu, byte[] logoBytes, String activite
    ) {}
    public record CoordonneesBancaires(String banque, String iban) {}
    public record LigneDocument(String description, String quantite, String prixUnitaire, String montant) {}
    public record Signature(String nomSignataire, String fonctionSignataire, byte[] cachetBytes) {}

    private record Contexte(
            String typeDocument, String numero, String statut, EnTeteEntreprise entreprise,
            String clientNom, List<String> clientLignes, String referenceDossier,
            String dateDocument, String dateEcheance, List<LigneDocument> lignes,
            BigDecimal montantHT, BigDecimal montantTaxe, BigDecimal montantTotal, BigDecimal montantPaye,
            String devise, String notes, String conditions, CoordonneesBancaires coordonneesBancaires,
            Signature signature, String mentionsLegales, Color accent
    ) {}

    private PdfDocumentBuilder() {}

    public static byte[] genererDocument(String templatePdf, String couleurAccentHex, String typeDocument, String numero,
                                         String statut, EnTeteEntreprise entreprise, String clientNom, List<String> clientLignes,
                                         String referenceDossier, String dateDocument, String dateEcheance,
                                         List<LigneDocument> lignes, BigDecimal montantHT, BigDecimal montantTaxe,
                                         BigDecimal montantTotal, BigDecimal montantPaye, String devise,
                                         String notes, String conditions, CoordonneesBancaires coordonneesBancaires,
                                         Signature signature, String mentionsLegales) {
        Color accent = parseCouleur(couleurAccentHex);
        Contexte ctx = new Contexte(typeDocument, numero, statut, entreprise, clientNom, clientLignes, referenceDossier,
                dateDocument, dateEcheance, lignes, montantHT, montantTaxe, montantTotal, montantPaye, devise, notes,
                conditions, coordonneesBancaires, signature, mentionsLegales, accent);

        Template template = parseTemplate(templatePdf);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 0, 0, 0, 0);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            document.newPage();

            switch (template) {
                case CLASSIQUE -> genererClassique(writer, document, ctx);
                case MINIMALISTE -> genererMinimaliste(writer, document, ctx);
                default -> genererModerne(writer, document, ctx);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de génération PDF", e);
        }
    }

    private static Template parseTemplate(String templatePdf) {
        if (templatePdf == null) return Template.MODERNE;
        try {
            return Template.valueOf(templatePdf.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Template.MODERNE;
        }
    }

    private static Color parseCouleur(String hex) {
        if (hex == null || hex.isBlank()) return DEFAULT_ACCENT;
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException e) {
            return DEFAULT_ACCENT;
        }
    }

    private static Color tint(Color base, float amount) {
        int r = (int) (base.getRed() + (255 - base.getRed()) * amount);
        int g = (int) (base.getGreen() + (255 - base.getGreen()) * amount);
        int b = (int) (base.getBlue() + (255 - base.getBlue()) * amount);
        return new Color(r, g, b);
    }

    private static void genererModerne(PdfWriter writer, Document document, Contexte ctx) {
        Color accentLight = tint(ctx.accent(), 0.88f);
        PdfContentByte cb = writer.getDirectContent();

        float headerH = 110;
        float yTop = PAGE_H;

        cb.setColorFill(accentLight);
        cb.rectangle(0, yTop - headerH, PAGE_W, headerH);
        cb.fill();

        float logoSize = 82;
        dessinerLogo(cb, ctx.entreprise(), ctx.accent(), MARGIN, yTop - headerH + (headerH - logoSize) / 2, logoSize);

        float textRightX = PAGE_W - MARGIN;
        texte(cb, FONT_BOLD, 17, TEXT_DARK, ctx.entreprise().nom() != null ? ctx.entreprise().nom() : "",
                textRightX, yTop - 42, Element.ALIGN_RIGHT);

        dessinerActivite(cb, ctx.entreprise().activite(), textRightX, yTop - 58, 260);

        float cutH = 46;
        float cutTop = yTop - headerH;
        float cutBottom = cutTop - cutH;
        float slant = 60;

        cb.setColorFill(accentLight);
        cb.moveTo(0, cutTop);
        cb.lineTo(PAGE_W, cutTop);
        cb.lineTo(PAGE_W, cutBottom);
        cb.lineTo(0, cutBottom - slant);
        cb.closePath();
        cb.fill();

        texte(cb, FONT_NORMAL, 22, ctx.accent(), ctx.typeDocument() + " " + ctx.numero(),
                textRightX, cutBottom - slant * 0.55f, Element.ALIGN_RIGHT);

        float y = cutBottom - slant;
        y = dessinerClient(cb, y, ctx.clientNom(), ctx.clientLignes());
        y = dessinerBarreInfos(cb, y, ctx, new Color(241, 245, 249));
        y = dessinerTableauAvecBandeau(cb, writer, document, y, ctx.lignes(), ctx.accent(), WHITE);
        y -= 20;
        cb = writer.getDirectContent();
        dessinerPiedTableau(cb, y, ctx, ctx.accent(), true);
        dessinerFooter(cb, ctx.entreprise(), ctx.mentionsLegales());
    }

    private static void genererClassique(PdfWriter writer, Document document, Contexte ctx) {
        PdfContentByte cb = writer.getDirectContent();
        float y = PAGE_H - MARGIN;

        float logoSize = 68;
        dessinerLogo(cb, ctx.entreprise(), ctx.accent(), MARGIN, y - logoSize, logoSize);

        texte(cb, FONT_BOLD, 15, TEXT_DARK, ctx.entreprise().nom() != null ? ctx.entreprise().nom() : "",
                PAGE_W - MARGIN, y - 16, Element.ALIGN_RIGHT);
        dessinerActivite(cb, ctx.entreprise().activite(), PAGE_W - MARGIN, y - 30, 260);

        y -= logoSize + 20;
        cb.setColorStroke(ctx.accent());
        cb.setLineWidth(2);
        cb.moveTo(MARGIN, y);
        cb.lineTo(PAGE_W - MARGIN, y);
        cb.stroke();
        y -= 30;

        texte(cb, FONT_BOLD, 20, TEXT_DARK, ctx.typeDocument() + " " + ctx.numero(), MARGIN, y, Element.ALIGN_LEFT);
        y -= 35;

        float yBlock = y;
        texte(cb, FONT_BOLD, 10, TEXT_DARK, ctx.clientNom(), MARGIN, yBlock, Element.ALIGN_LEFT);
        float yc = yBlock - 13;
        if (ctx.clientLignes() != null) {
            for (String ligne : ctx.clientLignes()) {
                if (ligne == null || ligne.isBlank()) continue;
                texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne, MARGIN, yc, Element.ALIGN_LEFT);
                yc -= 12;
            }
        }

        float infoX = PAGE_W - MARGIN - 180;
        float yi = yBlock;
        for (String[] info : infosList(ctx)) {
            texte(cb, FONT_NORMAL, 8.5f, TEXT_MUTED, info[0] + " :", infoX, yi, Element.ALIGN_LEFT);
            texte(cb, FONT_BOLD, 9, TEXT_DARK, info[1], PAGE_W - MARGIN, yi, Element.ALIGN_RIGHT);
            yi -= 14;
        }

        y = Math.min(yc, yi) - 20;
        y = dessinerTableauAvecBandeau(cb, writer, document, y, ctx.lignes(), ctx.accent(), WHITE);
        y -= 25;
        cb = writer.getDirectContent();
        dessinerPiedTableau(cb, y, ctx, ctx.accent(), false);
        dessinerFooter(cb, ctx.entreprise(), ctx.mentionsLegales());
    }

    private static void genererMinimaliste(PdfWriter writer, Document document, Contexte ctx) {
        PdfContentByte cb = writer.getDirectContent();
        float y = PAGE_H - MARGIN;

        float textX = MARGIN;
        if (ctx.entreprise().logoBytes() != null) {
            dessinerLogo(cb, ctx.entreprise(), ctx.accent(), MARGIN, y - 46, 46);
            textX = MARGIN + 58;
        }

        texte(cb, FONT_BOLD, 13, TEXT_DARK, ctx.entreprise().nom() != null ? ctx.entreprise().nom() : "",
                textX, y - 14, Element.ALIGN_LEFT);
        dessinerActiviteGauche(cb, ctx.entreprise().activite(), textX, y - 27, 300);
        texte(cb, FONT_NORMAL, 11, TEXT_DARK, ctx.typeDocument() + " " + ctx.numero(), PAGE_W - MARGIN, y - 14, Element.ALIGN_RIGHT);

        y -= 55;
        cb.setColorStroke(BORDER);
        cb.setLineWidth(0.5f);
        cb.moveTo(MARGIN, y);
        cb.lineTo(PAGE_W - MARGIN, y);
        cb.stroke();
        y -= 30;

        texte(cb, FONT_NORMAL, 10, TEXT_DARK, ctx.clientNom(), MARGIN, y, Element.ALIGN_LEFT);
        float yc = y - 13;
        if (ctx.clientLignes() != null) {
            for (String ligne : ctx.clientLignes()) {
                if (ligne == null || ligne.isBlank()) continue;
                texte(cb, FONT_NORMAL, 9, TEXT_MUTED, ligne, MARGIN, yc, Element.ALIGN_LEFT);
                yc -= 12;
            }
        }

        float infoX = PAGE_W - MARGIN - 160;
        float yi = y;
        for (String[] info : infosList(ctx)) {
            texte(cb, FONT_NORMAL, 8.5f, TEXT_MUTED, info[0], infoX, yi, Element.ALIGN_LEFT);
            texte(cb, FONT_NORMAL, 9, TEXT_DARK, info[1], PAGE_W - MARGIN, yi, Element.ALIGN_RIGHT);
            yi -= 13;
        }

        y = Math.min(yc, yi) - 30;

        float tableW = PAGE_W - 2 * MARGIN;
        float colDesc = tableW * 0.54f;
        float colQte = tableW * 0.12f;
        float colPu = tableW * 0.17f;

        texte(cb, FONT_NORMAL, 8, TEXT_MUTED, "DESCRIPTION", MARGIN, y, Element.ALIGN_LEFT);
        texte(cb, FONT_NORMAL, 8, TEXT_MUTED, "QTÉ", MARGIN + colDesc + colQte - 4, y, Element.ALIGN_RIGHT);
        texte(cb, FONT_NORMAL, 8, TEXT_MUTED, "PRIX UNIT.", MARGIN + colDesc + colQte + colPu - 4, y, Element.ALIGN_RIGHT);
        texte(cb, FONT_NORMAL, 8, TEXT_MUTED, "MONTANT", MARGIN + tableW, y, Element.ALIGN_RIGHT);
        y -= 8;
        cb.setColorStroke(TEXT_DARK);
        cb.setLineWidth(0.75f);
        cb.moveTo(MARGIN, y);
        cb.lineTo(MARGIN + tableW, y);
        cb.stroke();
        y -= 20;

        float minY = 150;
        for (LigneDocument ligne : ctx.lignes()) {
            if (y < minY) {
                document.newPage();
                cb = writer.getDirectContent();
                y = PAGE_H - 60;
            }
            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne.description(), MARGIN, y, Element.ALIGN_LEFT);
            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne.quantite(), MARGIN + colDesc + colQte - 4, y, Element.ALIGN_RIGHT);
            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne.prixUnitaire(), MARGIN + colDesc + colQte + colPu - 4, y, Element.ALIGN_RIGHT);
            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne.montant(), MARGIN + tableW, y, Element.ALIGN_RIGHT);
            y -= 22;
        }

        cb.setColorStroke(BORDER);
        cb.setLineWidth(0.5f);
        cb.moveTo(MARGIN, y + 6);
        cb.lineTo(MARGIN + tableW, y + 6);
        cb.stroke();
        y -= 20;

        if (ctx.conditions() != null && !ctx.conditions().isBlank()) {
            texte(cb, FONT_NORMAL, 9, TEXT_MUTED, ctx.conditions(), MARGIN, y, Element.ALIGN_LEFT);
        }

        float summaryX = PAGE_W - MARGIN - 180;
        float ry = y;
        for (Object[] row : totauxRows(ctx)) {
            String label = (String) row[0];
            String valeur = (String) row[1];
            String style = (String) row[2];
            if ("total".equals(style)) {
                texte(cb, FONT_BOLD, 10.5f, TEXT_DARK, label, summaryX, ry, Element.ALIGN_LEFT);
                texte(cb, FONT_BOLD, 10.5f, TEXT_DARK, valeur, PAGE_W - MARGIN, ry, Element.ALIGN_RIGHT);
            } else if ("du".equals(style)) {
                texte(cb, FONT_BOLD, 10.5f, ctx.accent(), label, summaryX, ry, Element.ALIGN_LEFT);
                texte(cb, FONT_BOLD, 10.5f, ctx.accent(), valeur, PAGE_W - MARGIN, ry, Element.ALIGN_RIGHT);
            } else if ("paye".equals(style)) {
                texte(cb, FONT_ITALIC, 9, SUCCESS, label, summaryX, ry, Element.ALIGN_LEFT);
                texte(cb, FONT_NORMAL, 9, TEXT_DARK, valeur, PAGE_W - MARGIN, ry, Element.ALIGN_RIGHT);
            } else {
                texte(cb, FONT_NORMAL, 9, TEXT_MUTED, label, summaryX, ry, Element.ALIGN_LEFT);
                texte(cb, FONT_NORMAL, 9, TEXT_DARK, valeur, PAGE_W - MARGIN, ry, Element.ALIGN_RIGHT);
            }
            ry -= 18;
        }

        cb.setColorStroke(ctx.accent());
        cb.setLineWidth(1.2f);
        cb.moveTo(summaryX, y + 12);
        cb.lineTo(PAGE_W - MARGIN, y + 12);
        cb.stroke();

        dessinerFooter(cb, ctx.entreprise(), ctx.mentionsLegales());
    }

    private static void dessinerActiviteGauche(PdfContentByte cb, String activite, float x, float yStart, float maxWidth) {
        if (activite == null || activite.isBlank()) return;
        List<String> lignes = decouperTexte(activite.toUpperCase(), FONT_NORMAL, 8, maxWidth);
        float y = yStart;
        int max = Math.min(lignes.size(), 2);
        for (int i = 0; i < max; i++) {
            texte(cb, FONT_NORMAL, 8, TEXT_MUTED, lignes.get(i), x, y, Element.ALIGN_LEFT);
            y -= 10;
        }
    }

    private static void dessinerLogo(PdfContentByte cb, EnTeteEntreprise entreprise, Color accent, float x, float y, float size) {
        if (entreprise.logoBytes() != null) {
            try {
                Image logo = Image.getInstance(entreprise.logoBytes());
                float scale = Math.min(size / logo.getWidth(), size / logo.getHeight());
                float w = logo.getWidth() * scale;
                float h = logo.getHeight() * scale;
                logo.setAbsolutePosition(x + (size - w) / 2, y + (size - h) / 2);
                logo.scaleAbsolute(w, h);
                cb.addImage(logo);
                return;
            } catch (Exception ignored) {
                // Image corrompue/illisible : on retombe sur le cercle avec initiale plutôt que d'échouer le PDF.
            }
        }
        cb.setColorFill(accent);
        cb.circle(x + size / 2, y + size / 2, size / 2);
        cb.fill();
        String initiale = (entreprise.nom() != null && !entreprise.nom().isBlank())
                ? entreprise.nom().substring(0, 1).toUpperCase() : "?";
        texte(cb, FONT_BOLD, size * 0.33f, WHITE, initiale, x + size / 2, y + size / 2 - size * 0.12f, Element.ALIGN_CENTER);
    }

    private static void dessinerActivite(PdfContentByte cb, String activite, float rightX, float yStart, float maxWidth) {
        if (activite == null || activite.isBlank()) return;
        List<String> lignes = decouperTexte(activite.toUpperCase(), FONT_NORMAL, 8.5f, maxWidth);
        float y = yStart;
        int max = Math.min(lignes.size(), 3);
        for (int i = 0; i < max; i++) {
            texte(cb, FONT_NORMAL, 8.5f, TEXT_MUTED, lignes.get(i), rightX, y, Element.ALIGN_RIGHT);
            y -= 11;
        }
    }
    private static float dessinerClient(PdfContentByte cb, float yStart, String clientNom, List<String> lignes) {
        float y = yStart - 40;
        texte(cb, FONT_BOLD, 11, TEXT_DARK, clientNom != null ? clientNom : "", MARGIN, y, Element.ALIGN_LEFT);
        y -= 14;
        if (lignes != null) {
            for (String ligne : lignes) {
                if (ligne == null || ligne.isBlank()) continue;
                texte(cb, FONT_NORMAL, 10, TEXT_DARK, ligne, MARGIN, y, Element.ALIGN_LEFT);
                y -= 13;
            }
        }
        return y;
    }

    private static String[][] infosList(Contexte ctx) {
        java.util.List<String[]> infos = new java.util.ArrayList<>();
        infos.add(new String[]{"Date", ctx.dateDocument() != null ? ctx.dateDocument() : "—"});
        if (ctx.dateEcheance() != null && !ctx.dateEcheance().isBlank()) infos.add(new String[]{"Échéance", ctx.dateEcheance()});
        if (ctx.referenceDossier() != null && !ctx.referenceDossier().isBlank()) infos.add(new String[]{"Dossier", ctx.referenceDossier()});
        return infos.toArray(new String[0][]);
    }

    private static float dessinerBarreInfos(PdfContentByte cb, float yStart, Contexte ctx, Color fond) {
        float y = yStart - 10;
        float infoH = 40;
        float infoTop = y;
        float infoBottom = y - infoH;
        float w = PAGE_W - 2 * MARGIN;

        cb.setColorFill(fond);
        cb.rectangle(MARGIN, infoBottom, w, infoH);
        cb.fill();

        java.util.List<String[]> infos = new java.util.ArrayList<>(java.util.Arrays.asList(infosList(ctx)));
        if (ctx.statut() != null && !ctx.statut().isBlank()) infos.add(new String[]{"Statut", libelleStatut(ctx.statut())});

        float colW = w / infos.size();
        for (int i = 0; i < infos.size(); i++) {
            float x = MARGIN + 14 + i * colW;
            texte(cb, FONT_BOLD, 8, TEXT_MUTED, infos.get(i)[0].toUpperCase(), x, infoTop - 15, Element.ALIGN_LEFT);
            Color valeurColor = "Statut".equals(infos.get(i)[0]) ? couleurStatut(ctx.statut()) : TEXT_DARK;
            texte(cb, FONT_NORMAL, 10, valeurColor, infos.get(i)[1], x, infoTop - 30, Element.ALIGN_LEFT);
        }

        return infoBottom - 25;
    }

    private static float dessinerTableauAvecBandeau(PdfContentByte cb, PdfWriter writer, Document document, float yStart,
                                                    List<LigneDocument> lignes, Color bandeau, Color texteBandeau) {
        float tableX = MARGIN;
        float tableW = PAGE_W - 2 * MARGIN;
        float colDesc = tableW * 0.52f;
        float colQte = tableW * 0.13f;
        float colPu = tableW * 0.17f;

        float headerRowH = 26;
        float rowH = 22;
        float minY = 120;

        float y = yStart;
        y = dessinerEnTeteTableau(cb, tableX, y, headerRowH, colDesc, colQte, colPu, bandeau, texteBandeau);

        int idx = 0;
        for (LigneDocument ligne : lignes) {
            if (y - rowH < minY) {
                texte(cb, FONT_ITALIC, 8, TEXT_MUTED, "(suite à la page suivante)", MARGIN, 40, Element.ALIGN_LEFT);
                document.newPage();
                cb = writer.getDirectContent();
                y = PAGE_H - 60;
                y = dessinerEnTeteTableau(cb, tableX, y, headerRowH, colDesc, colQte, colPu, bandeau, texteBandeau);
            }

            Color bg = (idx % 2 == 0) ? ROW_ALT : WHITE;
            cb.setColorFill(bg);
            cb.rectangle(tableX, y - rowH, tableW, rowH);
            cb.fill();

            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne.description(), tableX + 10, y - rowH + 7, Element.ALIGN_LEFT);
            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne.quantite(), tableX + colDesc + colQte - 8, y - rowH + 7, Element.ALIGN_RIGHT);
            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, ligne.prixUnitaire(), tableX + colDesc + colQte + colPu - 8, y - rowH + 7, Element.ALIGN_RIGHT);
            texte(cb, FONT_BOLD, 9.5f, TEXT_DARK, ligne.montant(), tableX + tableW - 8, y - rowH + 7, Element.ALIGN_RIGHT);

            y -= rowH;
            idx++;
        }

        cb.setColorStroke(BORDER);
        cb.setLineWidth(0.5f);
        cb.moveTo(tableX, y);
        cb.lineTo(tableX + tableW, y);
        cb.stroke();

        return y;
    }

    private static float dessinerEnTeteTableau(PdfContentByte cb, float tableX, float y, float headerRowH,
                                               float colDesc, float colQte, float colPu, Color bandeau, Color texteBandeau) {
        float tableW = PAGE_W - 2 * MARGIN;
        cb.setColorFill(bandeau);
        cb.rectangle(tableX, y - headerRowH, tableW, headerRowH);
        cb.fill();
        texte(cb, FONT_BOLD, 9, texteBandeau, "DESCRIPTION", tableX + 10, y - headerRowH + 9, Element.ALIGN_LEFT);
        texte(cb, FONT_BOLD, 9, texteBandeau, "QTÉ", tableX + colDesc + colQte - 8, y - headerRowH + 9, Element.ALIGN_RIGHT);
        texte(cb, FONT_BOLD, 9, texteBandeau, "PRIX UNITAIRE", tableX + colDesc + colQte + colPu - 8, y - headerRowH + 9, Element.ALIGN_RIGHT);
        texte(cb, FONT_BOLD, 9, texteBandeau, "MONTANT", tableX + tableW - 8, y - headerRowH + 9, Element.ALIGN_RIGHT);
        return y - headerRowH;
    }

    private static java.util.List<Object[]> totauxRows(Contexte ctx) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{"Montant HT", formatMontant(ctx.montantHT(), ctx.devise()), "normal"});
        if (ctx.montantTaxe() != null && ctx.montantTaxe().compareTo(BigDecimal.ZERO) > 0) {
            rows.add(new Object[]{"Taxes", formatMontant(ctx.montantTaxe(), ctx.devise()), "normal"});
        }
        rows.add(new Object[]{"Total", formatMontant(ctx.montantTotal(), ctx.devise()), "total"});
        if (ctx.montantPaye() != null && ctx.montantPaye().compareTo(BigDecimal.ZERO) > 0) {
            rows.add(new Object[]{"Payé", formatMontant(ctx.montantPaye(), ctx.devise()), "paye"});
            BigDecimal reste = ctx.montantTotal().subtract(ctx.montantPaye());
            rows.add(new Object[]{"Montant dû", formatMontant(reste, ctx.devise()), "du"});
        }
        return rows;
    }

    private static void dessinerPiedTableau(PdfContentByte cb, float yStart, Contexte ctx, Color accent, boolean avecEncadre) {
        float summaryW = 220;
        float summaryX = PAGE_W - MARGIN - summaryW;
        float y = yStart;

        float leftY = y;
        if (ctx.conditions() != null && !ctx.conditions().isBlank()) {
            texte(cb, FONT_NORMAL, 9.5f, TEXT_DARK, "Conditions de règlement : " + ctx.conditions(), MARGIN, leftY, Element.ALIGN_LEFT);
            leftY -= 20;
        }
        if (ctx.coordonneesBancaires() != null && ctx.coordonneesBancaires().banque() != null && !ctx.coordonneesBancaires().banque().isBlank()) {
            texte(cb, FONT_NORMAL, 9, TEXT_DARK, "Banque : " + ctx.coordonneesBancaires().banque(), MARGIN, leftY, Element.ALIGN_LEFT);
            leftY -= 14;
            if (ctx.coordonneesBancaires().iban() != null && !ctx.coordonneesBancaires().iban().isBlank()) {
                texte(cb, FONT_NORMAL, 9, TEXT_DARK, "IBAN : " + ctx.coordonneesBancaires().iban(), MARGIN, leftY, Element.ALIGN_LEFT);
                leftY -= 14;
            }
        }
        if (ctx.notes() != null && !ctx.notes().isBlank()) {
            texte(cb, FONT_NORMAL, 8.5f, TEXT_MUTED, ctx.notes(), MARGIN, leftY - 6, Element.ALIGN_LEFT);
        }

        java.util.List<Object[]> rows = totauxRows(ctx);
        float rowH2 = 26;
        float boxH = rowH2 * rows.size();
        float boxTop = y;

        if (avecEncadre) {
            cb.setColorStroke(BORDER);
            cb.setLineWidth(0.75f);
            cb.rectangle(summaryX, boxTop - boxH, summaryW, boxH);
            cb.stroke();
        }

        float ry = boxTop;
        for (Object[] row : rows) {
            ry -= rowH2;
            String label = (String) row[0];
            String valeur = (String) row[1];
            String style = (String) row[2];

            if ("total".equals(style)) {
                cb.setColorFill(accent);
                cb.rectangle(summaryX, ry, summaryW, rowH2);
                cb.fill();
                texte(cb, FONT_BOLD, 11.5f, WHITE, label, summaryX + 10, ry + 9, Element.ALIGN_LEFT);
                texte(cb, FONT_BOLD, 11.5f, WHITE, valeur, summaryX + summaryW - 10, ry + 9, Element.ALIGN_RIGHT);
            } else if ("du".equals(style)) {
                texte(cb, FONT_BOLD, 10, TEXT_DARK, label, summaryX + 10, ry + 9, Element.ALIGN_LEFT);
                texte(cb, FONT_BOLD, 10, TEXT_DARK, valeur, summaryX + summaryW - 10, ry + 9, Element.ALIGN_RIGHT);
            } else if ("paye".equals(style)) {
                texte(cb, FONT_ITALIC, 9, SUCCESS, label, summaryX + 10, ry + 9, Element.ALIGN_LEFT);
                texte(cb, FONT_NORMAL, 9, TEXT_DARK, valeur, summaryX + summaryW - 10, ry + 9, Element.ALIGN_RIGHT);
            } else {
                cb.setColorFill(ROW_ALT);
                cb.rectangle(summaryX, ry, summaryW, rowH2);
                cb.fill();
                texte(cb, FONT_NORMAL, 9, TEXT_MUTED, label, summaryX + 10, ry + 9, Element.ALIGN_LEFT);
                texte(cb, FONT_BOLD, 9, TEXT_DARK, valeur, summaryX + summaryW - 10, ry + 9, Element.ALIGN_RIGHT);
            }
            if (avecEncadre && ry > boxTop - boxH) {
                cb.setColorStroke(BORDER);
                cb.moveTo(summaryX, ry);
                cb.lineTo(summaryX + summaryW, ry);
                cb.stroke();
            }
        }

        Signature signature = ctx.signature();
        if (signature != null && signature.nomSignataire() != null && !signature.nomSignataire().isBlank()) {
            float sigY = Math.min(leftY - 30, boxTop - boxH - 20);
            if (signature.cachetBytes() != null) {
                try {
                    Image cachet = Image.getInstance(signature.cachetBytes());
                    float size = 60;
                    float scale = Math.min(size / cachet.getWidth(), size / cachet.getHeight());
                    cachet.setAbsolutePosition(MARGIN, sigY - size + 15);
                    cachet.scaleAbsolute(cachet.getWidth() * scale, cachet.getHeight() * scale);
                    cb.addImage(cachet);
                } catch (Exception ignored) { /* image illisible : on ignore silencieusement */ }
            }
            texte(cb, FONT_BOLD, 10, TEXT_DARK, signature.nomSignataire(), MARGIN + 75, sigY, Element.ALIGN_LEFT);
            if (signature.fonctionSignataire() != null) {
                texte(cb, FONT_NORMAL, 8, TEXT_MUTED, signature.fonctionSignataire(), MARGIN + 75, sigY - 13, Element.ALIGN_LEFT);
            }
        }
    }


    private static void dessinerFooter(PdfContentByte cb, EnTeteEntreprise entreprise, String mentionsLegales) {
        float footerY = 55;

        String identifiants = String.join("  \u2014  ", java.util.stream.Stream.of(
                entreprise.rccm() != null && !entreprise.rccm().isBlank() ? "RCCM " + entreprise.rccm() : null,
                entreprise.ifu() != null && !entreprise.ifu().isBlank() ? "IFU " + entreprise.ifu() : null,
                entreprise.email() != null && !entreprise.email().isBlank() ? entreprise.email() : null,
                entreprise.siteWeb() != null && !entreprise.siteWeb().isBlank() ? entreprise.siteWeb() : null
        ).filter(java.util.Objects::nonNull).toList());

        // Le texte des mentions légales peut être long — on le découpe en lignes qui
        // tiennent dans la largeur de la page plutôt que de le forcer sur une seule ligne
        // (ce qui déborderait ou serait illisible pour un texte de plusieurs centaines de caractères).
        List<String> lignesMentions = mentionsLegales != null && !mentionsLegales.isBlank()
                ? decouperTexte(mentionsLegales, FONT_NORMAL, 7.5f, PAGE_W - 2 * MARGIN - 20)
                : List.of();

        float footerH = 12 + (identifiants.isBlank() ? 0 : 10) + lignesMentions.size() * 10;
        float ligneY = footerY + footerH;

        cb.setColorStroke(BORDER);
        cb.setLineWidth(0.5f);
        cb.moveTo(MARGIN, ligneY);
        cb.lineTo(PAGE_W - MARGIN, ligneY);
        cb.stroke();

        float fy = ligneY - 12;
        if (!identifiants.isBlank()) {
            texte(cb, FONT_NORMAL, 7.5f, TEXT_MUTED, identifiants, PAGE_W / 2, fy, Element.ALIGN_CENTER);
            fy -= 10;
        }
        for (String ligne : lignesMentions) {
            texte(cb, FONT_NORMAL, 7.5f, TEXT_MUTED, ligne, PAGE_W / 2, fy, Element.ALIGN_CENTER);
            fy -= 10;
        }

        texte(cb, FONT_NORMAL, 8, TEXT_MUTED, "Page 1", PAGE_W - MARGIN, footerY, Element.ALIGN_RIGHT);
    }

    /** Découpe un texte en lignes dont la largeur (en points, à la taille de police donnée) ne dépasse pas maxWidth. */
    private static List<String> decouperTexte(String texte, BaseFont font, float taille, float maxWidth) {
        List<String> lignes = new java.util.ArrayList<>();
        StringBuilder ligneActuelle = new StringBuilder();
        for (String mot : texte.trim().split("\\s+")) {
            String essai = ligneActuelle.isEmpty() ? mot : ligneActuelle + " " + mot;
            if (font.getWidthPoint(essai, taille) > maxWidth && !ligneActuelle.isEmpty()) {
                lignes.add(ligneActuelle.toString());
                ligneActuelle = new StringBuilder(mot);
            } else {
                ligneActuelle = new StringBuilder(essai);
            }
        }
        if (!ligneActuelle.isEmpty()) lignes.add(ligneActuelle.toString());
        return lignes;
    }


    private static void texte(PdfContentByte cb, BaseFont font, float size, Color color, String texte, float x, float y, int align) {
        if (texte == null) texte = "";
        cb.beginText();
        cb.setFontAndSize(font, size);
        cb.setColorFill(color);
        cb.showTextAligned(align, texte, x, y, 0);
        cb.endText();
    }

    private static Color couleurStatut(String statut) {
        if (statut == null) return TEXT_MUTED;
        return switch (statut) {
            case "PAYEE", "ACCEPTEE", "VALIDE" -> SUCCESS;
            case "ANNULEE", "REFUSEE", "EN_RETARD" -> DANGER;
            default -> TEXT_DARK;
        };
    }

    private static String libelleStatut(String statut) {
        return switch (statut) {
            case "BROUILLON" -> "Brouillon";
            case "EMISE" -> "Émise";
            case "PARTIELLEMENT_PAYEE" -> "Partiellement payée";
            case "PAYEE" -> "Payée";
            case "EN_RETARD" -> "En retard";
            case "ANNULEE" -> "Annulée";
            case "ENVOYEE" -> "Envoyée";
            case "ACCEPTEE" -> "Acceptée";
            case "REFUSEE" -> "Refusée";
            default -> statut;
        };
    }

    private static final java.text.DecimalFormat MONTANT_FORMAT = creerFormatMontant();

    private static java.text.DecimalFormat creerFormatMontant() {
        java.text.DecimalFormatSymbols symboles = new java.text.DecimalFormatSymbols(java.util.Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator('.');
        return new java.text.DecimalFormat("#,##0.00", symboles);
    }

    private static String formatMontant(BigDecimal montant, String devise) {
        String valeur = montant != null ? MONTANT_FORMAT.format(montant) : "0.00";
        return valeur + " " + (devise != null ? devise : "");
    }

    /** Exposé publiquement pour que FacturePdfService/CotationPdfService formatent aussi les lignes du tableau (mêmes espaces de milliers). */
    public static String formatNombre(BigDecimal valeur) {
        return valeur != null ? MONTANT_FORMAT.format(valeur) : "0.00";
    }
}