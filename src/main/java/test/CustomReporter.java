package test;

import org.testng.*;
import org.testng.collections.Lists;
import org.testng.log4testng.Logger;
import org.testng.xml.XmlSuite;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class CustomReporter implements IReporter {

    private static final Logger L = Logger.getLogger(CustomReporter.class);

    private PrintWriter m_out;

    private int m_row;

    private int m_methodIndex;

    @SuppressWarnings("unused")
    private int m_rowTotal;

    private final File emailableReportDir;
    public CustomReporter()
    {
        File file = new File("");
        File targetDir = new File(file.getAbsolutePath() + File.separator + "target");
        emailableReportDir = new File(targetDir.getAbsolutePath() + File.separator + "emailable-report");
    }


    /** Creates summary of the run */
    @Override
    public void generateReport(List<XmlSuite> xml, List<ISuite> suites, String outdir)
    {
        // Overriding output dir
        outdir = emailableReportDir.getAbsolutePath();
        try
        {
            m_out = createWriter(outdir);
        }
        catch (IOException e)
        {
            L.error("output file", e);
            return;
        }
        startHtml(m_out);
        generateSuiteSummaryReport(suites);
//        generateMethodSummaryReport(suites);
//        generateMethodDetailReport(suites);
        endHtml(m_out);
        m_out.flush();
        m_out.close();
    }
    protected PrintWriter createWriter(String outdir) throws IOException
    {
        new File(outdir).mkdirs();
        return new PrintWriter(new BufferedWriter(new FileWriter(new File(outdir, "emailable-report.html"))));
    }

    /** Starts HTML stream */
    protected void startHtml(PrintWriter out)
    {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        out.println("<head>");
        out.println("<title>TestNG:  Unit Test</title>");
        out.println("<style type=\"text/css\">");
        out.println("table caption,table.info_table,table.result,table.passed,table.failed {margin-bottom:10px;border:1px solid #000099;border-collapse:collapse;empty-cells:show;}");
        out.println("table.info_table td,table.info_table th,table.result td,table.result th,table.passed td,table.passed th,table.failed td,table.failed th {");
        out.println("border:1px solid #000099;padding:.25em .5em .25em .5em");
        out.println("}");
        out.println("table.result th {vertical-align:bottom}");
        out.println("tr.param th {padding-left:1em;padding-right:1em}");
        out.println("tr.param td {padding-left:.5em;padding-right:2em}");
        out.println("td.numi,th.numi,td.numi_attn {");
        out.println("text-align:right");
        out.println("}");
        out.println("tr.total td {font-weight:bold}");
        out.println("table caption {");
        out.println("text-align:center;font-weight:bold;");
        out.println("}");
        out.println("table.passed tr.stripe td,table tr.passedodd td {background-color: #00AA00;}");
        out.println("table.passed td,table tr.passedeven td {background-color: #33FF33;}");
        out.println("table.passed tr.stripe td,table tr.skippedodd td {background-color: #cccccc;}");
        out.println("table.passed td,table tr.skippedodd td {background-color: #dddddd;}");
        out.println("table.failed tr.stripe td,table tr.failedodd td,table.result td.numi_attn {background-color: #FF3333;}");
        out.println("table.failed td,table tr.failedeven td,table.result tr.stripe td.numi_attn {background-color: #DD0000;}");
        out.println("tr.stripe td,tr.stripe th {background-color: #E6EBF9;}");
        out.println("p.totop {font-size:85%;text-align:center;border-bottom:2px black solid}");
        out.println("div.shootout {padding:2em;border:3px #4854A8 solid}");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
    }

    /** Finishes HTML stream */
    protected void endHtml(PrintWriter out)
    {
        out.println("</body></html>");
    }

    private void tableStart(String cssclass, String id)
    {
        m_out.println("<table cellspacing=\"0\" cellpadding=\"0\"" + (cssclass != null ? " class=\"" + cssclass + "\"" : " style=\"padding-bottom:2em\"") + (id != null ? " id=\"" + id + "\"" : "") + ">");
        m_row = 0;
    }
    private void tableColumnStart(String label)
    {
        m_out.print("<th class=\"numi\">" + label + "</th>");
    }

    private void titleRow(String label, int cq)
    {
        m_out.println("<tr><th colspan=\"" + cq + "\">" + label + "</th></tr>");
        m_row = 0;
    }

    private void startSummaryRow(String label)
    {
        m_row += 1;
        m_out.print("<tr" + (m_row % 2 == 0 ? " class=\"stripe\"" : "") + "><td style=\"text-align:left;padding-right:2em\">" + label + "</td>");
    }

    private void summaryCell(String[] val)
    {
        StringBuffer b = new StringBuffer();
        for (String v : val)
        {
            b.append(v + " ");
        }
        summaryCell(b.toString(), true);
    }

    private void summaryCell(String v, boolean isgood)
    {
        m_out.print("<td class=\"numi" + (isgood ? "" : "_attn") + "\">" + v + "</td>");
    }

    private void summaryCell(int v, int maxexpected)
    {
        summaryCell(String.valueOf(v), v <= maxexpected);
        m_rowTotal += v;
    }

    public void generateSuiteSummaryReport(List<ISuite> suites)
    {
        tableStart("result", null);
        m_out.print("<tr><th>Test</th>");
        tableColumnStart("Methods<br/>Passed");
        tableColumnStart("Scenarios<br/>Passed");
        tableColumnStart("# skipped");
        tableColumnStart("# failed");
        tableColumnStart("Total<br/>Time");
        tableColumnStart("Included<br/>Groups");
        tableColumnStart("Excluded<br/>Groups");
        m_out.println("</tr>");
        NumberFormat formatter = new DecimalFormat("#,##0.0");
        int qty_tests = 0;
        int qty_pass_m = 0;
        int qty_pass_s = 0;
        int qty_skip = 0;
        int qty_fail = 0;
        long time_start = Long.MAX_VALUE;
        long time_end = Long.MIN_VALUE;
        for (ISuite suite : suites)
        {
            if (suites.size() > 1)
            {
                titleRow(suite.getName(), 7);
            }
            Map<String, ISuiteResult> tests = suite.getResults();
            for (ISuiteResult r : tests.values())
            {
                qty_tests += 1;
                ITestContext overview = r.getTestContext();
                startSummaryRow(overview.getName());
                int q = getMethodSet(overview.getPassedTests(), suite).size();
                qty_pass_m += q;
                summaryCell(q, Integer.MAX_VALUE);
                q = overview.getPassedTests().size();
                qty_pass_s += q;
                summaryCell(q, Integer.MAX_VALUE);
                q = getMethodSet(overview.getSkippedTests(), suite).size();
                qty_skip += q;
                summaryCell(q, 0);
                q = getMethodSet(overview.getFailedTests(), suite).size();
                qty_fail += q;
                summaryCell(q, 0);
                time_start = Math.min(overview.getStartDate().getTime(), time_start);
                time_end = Math.max(overview.getEndDate().getTime(), time_end);
                summaryCell(formatter.format((overview.getEndDate().getTime() - overview.getStartDate().getTime()) / 1000.) + " seconds", true);
                summaryCell(overview.getIncludedGroups());
                summaryCell(overview.getExcludedGroups());
                m_out.println("</tr>");
            }
        }
        if (qty_tests > 1)
        {
            m_out.println("<tr class=\"total\"><td>Total</td>");
            summaryCell(qty_pass_m, Integer.MAX_VALUE);
            summaryCell(qty_pass_s, Integer.MAX_VALUE);
            summaryCell(qty_skip, 0);
            summaryCell(qty_fail, 0);
            summaryCell(formatter.format((time_end - time_start) / 1000.) + " seconds", true);
            m_out.println("<td colspan=\"2\">&nbsp;</td></tr>");
        }
        m_out.println("</table>");
    }

    /**
     * Since the methods will be sorted chronologically, we want to return the
     * ITestNGMethod from the invoked methods.
     */
    private Collection<ITestNGMethod> getMethodSet(IResultMap tests, ISuite suite)
    {
        List<IInvokedMethod> r = Lists.newArrayList();
        List<IInvokedMethod> invokedMethods = suite.getAllInvokedMethods();
        for (IInvokedMethod im : invokedMethods)
        {
            if (tests.getAllMethods().contains(im.getTestMethod()))
            {
                r.add(im);
            }
        }
        Arrays.sort(r.toArray(new IInvokedMethod[r.size()]), new TestSorter());
        List<ITestNGMethod> result = Lists.newArrayList();

        // Add all the invoked methods
        for (IInvokedMethod m : r)
        {
            result.add(m.getTestMethod());
        }

        // Add all the methods that weren't invoked (e.g. skipped) that we
        // haven't added yet
        for (ITestNGMethod m : tests.getAllMethods())
        {
            if (!result.contains(m))
            {
                result.add(m);
            }
        }
        return result;
    }

    // ~ Inner Classes --------------------------------------------------------
    /** Arranges methods by classname and method name */
    private class TestSorter implements Comparator<IInvokedMethod>
    {
        // ~ Methods
        // -------------------------------------------------------------

        /** Arranges methods by classname and method name */
        @Override
        public int compare(IInvokedMethod o1, IInvokedMethod o2)
        {
            // System.out.println("Comparing " + o1.getMethodName() + " " +
            // o1.getDate()
            // + " and " + o2.getMethodName() + " " + o2.getDate());
            return (int) (o1.getDate() - o2.getDate());
            // int r = ((T) o1).getTestClass().getName().compareTo(((T)
            // o2).getTestClass().getName());
            // if (r == 0) {
            // r = ((T) o1).getMethodName().compareTo(((T) o2).getMethodName());
            // }
            // return r;
        }
    }
}



