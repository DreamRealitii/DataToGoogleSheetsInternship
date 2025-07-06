import com.google.api.client.http.HttpTransport;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.*;
import com.google.api.services.sheets.v4.model.*;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.text.SimpleDateFormat;

//The class that runs uploads to Google Sheets
public class SheetsQuickstart {
    private static final String APPLICATION_NAME = "Raspberry Pi Sensor Data Upload";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static Sheets service;
    private static int filterId = 1;
    private static int[] dimensions;

    //RRun this at the beginning of the program to "unlock" spreadsheet operations, will open browser the first time it runs.
    public static void setup() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(APPLICATION_NAME).build();
    }

    //Creates a spreadsheet, then returns its ID into a text file to be read later when "updateSpreadsheet" is called.
    public static String createSpreadsheet(String filePath) throws IOException, GeneralSecurityException {
        dimensions = new Csv(filePath).dimensions;

        //Creates all the sheets.
        List<Sheet> sheets = new ArrayList<>();
        SheetProperties rawSheet = new SheetProperties().setTitle("Raw Data").setSheetId(1);
        SheetProperties timeSheet = new SheetProperties().setTitle("Time Data").setSheetId(2);
        SheetProperties tableSheet = new SheetProperties().setTitle("Data Table").setSheetId(3);
        SheetProperties chartSheet = new SheetProperties().setTitle("Main Graph").setSheetId(4).setSheetType("OBJECT");
        sheets.add(new Sheet().setProperties(rawSheet));
        sheets.add(new Sheet().setProperties(timeSheet));
        sheets.add(new Sheet().setProperties(tableSheet));
        sheets.add(new Sheet().setProperties(chartSheet).setCharts(createMainGraph(dimensions[1])));
        
        //Creates a spreadsheet out of the created sheets.
        Spreadsheet sheet = new Spreadsheet().setSheets(sheets);
        Spreadsheet create = null;
        Sheets.Spreadsheets.Create createSheet = service.spreadsheets().create(sheet);
        create = createSheet.execute();

        //Sets up list of "other things" to do to create the spreadsheet.
        List<Request> changes = new ArrayList<>();
        changes.add(new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest().setFields("title").setProperties(new SheetProperties().setTitle("Raw Graph").setSheetId(4))));
        List<Request> filters = allFilters();
        for(Request r : filters)
            changes.add(r);

        //Does the "other things".
        BatchUpdateSpreadsheetRequest updates = new BatchUpdateSpreadsheetRequest().setRequests(changes);
        BatchUpdateSpreadsheetResponse aaa = service.spreadsheets().batchUpdate(create.getSpreadsheetId(), updates).execute();
        System.out.println("Spreadsheet Created.");

        //Writes and returns new spreadsheet's ID.
        SID.writeTextFile(create.getSpreadsheetId(), "spreadsheetID");
        return create.getSpreadsheetId();
    }

    //Writes data into a spreadsheet with specified "ID", with data from a .csv file located by "filePath".
    public static void updateSpreadsheet(String filePath, String id) throws IOException, GeneralSecurityException {
        //Gets data from Csv file and plugs it into a ValueRange.
        String[][] data = new Csv(filePath).getValues();
        dimensions = new int[]{data.length, data[0].length};
        String dataRange = "Raw Data!A1:" + getRange(data, 0);
        ValueRange dataValueRange = new ValueRange().setMajorDimension("ROWS").setRange(dataRange).setValues(Csv.arrayToList(data));

        //Plugs ValueRange into spreadsheet specified by "ID".
        Sheets.Spreadsheets.Values.Update setData = service.spreadsheets().values().update(id, dataRange, dataValueRange);
        UpdateValuesResponse addData = setData.setValueInputOption("USER_ENTERED").execute();
        
        //This plugs the different parts of time measurement (minutes, days, etc) into the Time Data sheet.
        timeSheet(data, id);   

        //Sets up list of other updates to make to the spreadsheet.
        List<Request> changes = new ArrayList<>();
        UpdateSpreadsheetPropertiesRequest changeTitle = new UpdateSpreadsheetPropertiesRequest().setProperties(new SpreadsheetProperties()
        .setTitle("Data Recorded From PI at Time: " + new SimpleDateFormat().format(new Date(System.currentTimeMillis())))).setFields("title");
        changes.add(new Request().setUpdateSpreadsheetProperties(changeTitle));
        changes.add(pivotTable(dimensions));

        //Performs list of updates, prints message if successful.
        BatchUpdateSpreadsheetRequest updates = new BatchUpdateSpreadsheetRequest().setRequests(changes);
        BatchUpdateSpreadsheetResponse aaa = service.spreadsheets().batchUpdate(id, updates).execute();
        System.out.println("Spreadsheet Updated.");
    }

    //Takes in the .csv data and spreadsheet ID. 
    //Processes the date/time column and plugs in the separate time indicators (time, day, month, etc.) into their own columns on the Time sheet.
    //The first column is the full time/date in the default Google Sheets format (Month/Day/Year Hour:Minute:Second).
    public static void timeSheet(String[][] data, String id) throws IOException, GeneralSecurityException {
        TimeData timing = new TimeData(data);
        String timeRange = "Time Data!A1:" + getRange(timing.getData(), 1);
        ValueRange timeValueRange = new ValueRange().setMajorDimension("ROWS").setRange(timeRange).setValues(timing.getData());
        Sheets.Spreadsheets.Values.Update setTimes = service.spreadsheets().values().update(id, timeRange, timeValueRange);
        UpdateValuesResponse addTimes = setTimes.setValueInputOption("USER_ENTERED").execute();
    }

    //Creates the pivot table sheet.
    public static Request pivotTable(int[] dimensions){
        List<PivotGroup> pivotRows = new ArrayList<>();
        List<PivotValue> pivotValues = new ArrayList<>();
        pivotRows.add(new PivotGroup().setSourceColumnOffset(8).setShowTotals(true).setSortOrder("ASCENDING"));
        pivotRows.add(new PivotGroup().setSourceColumnOffset(7).setShowTotals(true).setSortOrder("ASCENDING"));
        pivotRows.add(new PivotGroup().setSourceColumnOffset(6).setShowTotals(true).setSortOrder("ASCENDING"));
        pivotRows.add(new PivotGroup().setSourceColumnOffset(4).setShowTotals(true).setSortOrder("ASCENDING"));
        pivotRows.add(new PivotGroup().setSourceColumnOffset(3).setShowTotals(true).setSortOrder("ASCENDING"));
        for(int i = 1; i < dimensions[1]; i++){
            pivotValues.add(new PivotValue().setSourceColumnOffset(i+8).setSummarizeFunction("AVERAGE").setName("Avg of Data " + i));
            pivotValues.add(new PivotValue().setSourceColumnOffset(i+8).setSummarizeFunction("STDEV").setName("STD of Data " + i));
            pivotValues.add(new PivotValue().setSourceColumnOffset(i+8).setSummarizeFunction("MAX").setName("Max of Data " + i));
            pivotValues.add(new PivotValue().setSourceColumnOffset(i+8).setSummarizeFunction("MIN").setName("Min of Data " + i));
        }

        PivotTable table = new PivotTable().setRows(pivotRows).setValues(pivotValues).setSource(new GridRange().setSheetId(2).setEndColumnIndex(dimensions[1] + 8).setEndRowIndex(dimensions[0] + 1)).setValueLayout("VERTICAL");
        List<CellData> cells = new ArrayList<>();
        cells.add(new CellData().setPivotTable(table));
        List<RowData> rows = new ArrayList<>();
        rows.add(new RowData().setValues(cells));
        return new Request().setUpdateCells(new UpdateCellsRequest().setRows(rows).setRange(new GridRange().setSheetId(3)).setFields("pivotTable"));
    }

    //Adds a graph based on raw data to the graph sheet. It's insanity.
    public static List<EmbeddedChart> createMainGraph(int columns){
        List<EmbeddedChart> result = new ArrayList<>();

        //Creates axis of the graph.
        List<BasicChartAxis> axis = new ArrayList<>();
        axis.add(new BasicChartAxis().setTitle("Time").setPosition("BOTTOM_AXIS"));
        axis.add(new BasicChartAxis().setTitle("Data").setPosition("LEFT_AXIS"));

        //Creates domain (x-axis) of graph.
        List<GridRange> range = new ArrayList<>();
        range.add(new GridRange().setSheetId(2).setEndColumnIndex(1));
        List<BasicChartDomain> domain = new ArrayList<>();
        domain.add(new BasicChartDomain().setDomain(new ChartData().setSourceRange(new ChartSourceRange().setSources(range))));

        //Creates a data source for each column to plug into the graph.
        List<List<GridRange>> dataRanges = new ArrayList<>();
        List<BasicChartSeries> series = new ArrayList<>();
        for(int i = 1; i < columns; i++){
            dataRanges.add(new ArrayList<GridRange>());
            dataRanges.get(i-1).add(new GridRange().setSheetId(2).setStartColumnIndex(i+8).setEndColumnIndex(i+9));
            series.add(new BasicChartSeries().setSeries(new ChartData().setSourceRange(new ChartSourceRange().setSources(dataRanges.get(i-1)))).setType("LINE"));
        }

        result.add(new EmbeddedChart().setPosition(new EmbeddedObjectPosition().setSheetId(4)).setSpec(new ChartSpec().setTitle("Main Graph").setMaximized(true)
                .setBasicChart(new BasicChartSpec().setChartType("LINE").setDomains(domain).setSeries(series).setAxis(axis).setHeaderCount(1))));
        return result;
    }

    //This is where I create all the filters instead of putting it all in the spreadsheet creation method.
    public static List<Request> allFilters(){
        List<Request> result = new ArrayList<>();

        result.add(filter("TEXT_CONTAINS", "Show Data From Sunday", "1", 5, false));
        result.add(filter("TEXT_CONTAINS", "Show Data From Monday", "2", 5, false));
        result.add(filter("TEXT_CONTAINS", "Show Data From Tuesday", "3", 5, false));
        result.add(filter("TEXT_CONTAINS", "Show Data From Wednesday", "4", 5, false));
        result.add(filter("TEXT_CONTAINS", "Show Data From Thursday", "5", 5, false));
        result.add(filter("TEXT_CONTAINS", "Show Data From Friday", "6", 5, false));
        result.add(filter("TEXT_CONTAINS", "Show Data From Saturday", "7", 5, false));

        result.add(filter("NUMBER_LESS", "Show Data From A.M. Times", "12", 4, false));
        result.add(filter("NUMBER_GREATER", "Show Data From P.M. Times", "11", 4, false));

        result.add(filter("DATE_AFTER", "Show Data From Today", "TODAY", 1, true));
        result.add(filter("DATE_AFTER", "Show Data From Past Week", "PAST_WEEK", 1, true));
        result.add(filter("DATE_AFTER", "Show Data From Past Month", "PAST_MONTH", 1, true));
        result.add(filter("DATE_AFTER", "Show Data From Past Year", "PAST_YEAR", 1, true));

        result.add(filter("NUMBER_LESS", "Show Data From First Week of Month", "8", 7, false));
        result.add(betweenFilter("NUMBER_BETWEEN", "Show Data From Second Week of Month", "8", "14", 7));
        result.add(betweenFilter("NUMBER_BETWEEN", "Show Data From Third Week of Month", "15", "21", 7));
        result.add(betweenFilter("NUMBER_BETWEEN", "Show Data From Fourth Week of Month", "22", "28", 7));
        result.add(filter("NUMBER_GREATER", "Show Data From Fifth Week of Month", "28", 7, false));

        for(int i = 1; i <= 24; i++)
            result.add(filter("NUMBER_EQ", "Show Data From Hour " + String.valueOf(i), String.valueOf(i), 4, false));
        for(int i = 1; i <= 12; i++)
            result.add(filter("NUMBER_EQ", "Show Data From Month " + String.valueOf(i), String.valueOf(i), 8, false));

        return result;
    }

    //Creates a "Filter View" that filters data inside the Time Data sheet.
    public static Request filter(String filterType, String title, String number, int column, boolean relativeDate){
        FilterView result = new FilterView().setTitle(title + ".").setFilterViewId(filterId);
        filterId++;
        GridRange range = new GridRange().setSheetId(2).setStartColumnIndex(column-1).setEndColumnIndex(column);
        List<ConditionValue> value = new ArrayList<>();
        if(relativeDate)
            value.add(new ConditionValue().setRelativeDate(number));
        else value.add(new ConditionValue().setUserEnteredValue(number));
        Map<String, FilterCriteria> filter = new HashMap<>();
        filter.put(String.valueOf(column-1), new FilterCriteria().setCondition(new BooleanCondition().setType(filterType).setValues(value)));    
        return new Request().setAddFilterView(new AddFilterViewRequest().setFilter(result.setRange(range).setCriteria(filter)));
    }

    //Filter that filters data in between two values.
    public static Request betweenFilter(String filterType, String title, String number1, String number2, int column){
        FilterView result = new FilterView().setTitle(title + ".").setFilterViewId(filterId);
        filterId++;
        GridRange range = new GridRange().setSheetId(2).setStartColumnIndex(column-1).setEndColumnIndex(column);
        List<ConditionValue> value = new ArrayList<>();
        value.add(new ConditionValue().setUserEnteredValue(number1));
        value.add(new ConditionValue().setUserEnteredValue(number2));
        Map<String, FilterCriteria> filter = new HashMap<>();
        filter.put(String.valueOf(column-1), new FilterCriteria().setCondition(new BooleanCondition().setType(filterType).setValues(value)));    
        return new Request().setAddFilterView(new AddFilterViewRequest().setFilter(result.setRange(range).setCriteria(filter)));
    }

    //First character is letter representing # of columns.
    //Second character is number representing # of rows.
    //Input 1 into offset if you have a labeled row on top.
    public static String getRange(Object[][] arr, int offset){
        char column = (char)(arr[offset].length + 64);
        return String.valueOf((char)column) + String.valueOf(arr.length + offset);
    }
    public static String getRange(List<List<Object>> arr, int offset){
        char column = (char)(arr.get(0).size() + 64);
        return String.valueOf((char)column) + String.valueOf(arr.size() + offset);
    }

    //Leftover method that I need from the sample code.
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = SheetsQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH))).setAccessType("offline").build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
}