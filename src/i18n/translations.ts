import { Language } from '../types';

export const translations = {
  en: {
    appTitle: 'KisanSarthi MP',
    appSubtitle: 'Smart Mandi Procurement & Queue Management System',
    govtTag: 'Govt. of Madhya Pradesh • Farmer Welfare & APMC Board',
    tagline: 'Zero Wait Mandi Slots • Transparent MSP • Instant DBT',
    
    // Auth & Roles
    loginAsFarmer: 'Farmer Login (Kisan)',
    loginAsAdmin: 'Admin Login (APMC / Mandi Officer)',
    loginDescription: 'Secure OTP-based authentication for farmers and mandi officers',
    mobileNumber: 'Mobile Number / Kisan Samagra ID',
    enterOtp: 'Enter 4-Digit OTP',
    verifyOtp: 'Verify & Access Mandi Portal',
    resendOtp: 'Resend SMS OTP',
    switchRole: 'Switch Role',
    adminKey: 'APMC Officer Passcode',
    demoQuickLogin: 'Quick Demo Access',
    asFarmer: 'Login as Farmer (Ramesh Patel - Sehore)',
    asAdmin: 'Login as Mandi Officer (Sehore APMC)',
    logout: 'Logout',
    loggedInAs: 'Logged in as',

    // Tabs
    tabActiveToken: 'My Live Token',
    tabBookSlot: 'Book Mandi Slot',
    tabProfitCalc: 'Yield & Profit Calc',
    tabMandiQueues: 'Live Mandi Status',
    tabMspRates: 'Today MSP & Rates',
    tabWeather: 'Weather & Harvest',
    tabAdminCapacity: 'Mandi Capacity & Queue',
    tabAdminProcurement: 'Procurement Slips & DBT',
    tabAdminReports: 'Statewide Analytics',

    // Booking & Queue
    selectDistrict: 'Select District (MP)',
    selectMandi: 'Select APMC Mandi Center',
    selectCrop: 'Select Commodity / Crop',
    estimatedYield: 'Total Estimated Yield (Quintals)',
    acreage: 'Farm Acreage (Acres)',
    harvestDate: 'Harvest Completion Date',
    selectSlot: 'Select Time Slot',
    vehicleType: 'Transport Vehicle',
    vehicleNumber: 'Vehicle / Tractor Number',
    aiSlotRecommendation: 'AI Logistical Slot Recommendation',
    aiSlotDesc: 'Analyzed historical mandi congestion, weighbridge capacity & weather.',
    recommendedSlotBadge: 'AI Low-Wait Slot',
    bookSlotBtn: 'Confirm & Generate e-Token',
    bookingSuccess: 'Slot Booked Successfully!',
    tokenNumber: 'e-Token Number',
    estimatedWait: 'Estimated Wait Time',
    tokensAhead: 'Tokens Ahead in Queue',
    nowServing: 'Now Serving Token',
    weighbridgesActive: 'Active Weighbridges',
    gateStatus: 'Gate Status',

    // Steps
    stepBooked: 'Slot Confirmed',
    stepGateEntered: 'Gate Entry Done',
    stepWeighbridgeGross: 'Gross Weighbridge',
    stepQcInspection: 'Moisture & Quality QC',
    stepUnloading: 'Unloading at Shed',
    stepWeighbridgeTare: 'Tare Weight',
    stepCompleted: 'J-Form Slip Issued',

    // Payments
    dbtStatus: 'DBT Payment Status',
    payoutAmount: 'Procurement Amount (MSP)',
    utrNumber: 'DBT Reference UTR',
    bankAccount: 'Bank Account Linked',
    viewJForm: 'View Official J-Form Receipt',
    printPass: 'Print / Download Gate Pass',
    
    // Profit Calc
    calculatorTitle: 'Crop Yield, MSP Payout & Profit Margin Estimator',
    calculatorSubtitle: 'Calculate input expenditures vs guaranteed MSP return with state bonus',
    inputCostPerAcre: 'Input Cost per Acre (Seeds, Fertilizers, Labor, Diesel)',
    totalInputCost: 'Total Cultivation Expenditure',
    grossRevenueMsp: 'Gross Revenue at Mandi MSP',
    projectedNetProfit: 'Projected Net Profit',
    profitMargin: 'Profit Margin',
    aiYieldAdvisorBtn: 'Run AI Yield & Margin Advisor',

    // Offline
    offlineBadge: 'Offline Mode (No Internet)',
    offlineDesc: 'Data stored locally on device. Will auto-sync when connected.',
    syncPendingCount: 'Action(s) queued for sync',
    onlineSynced: 'Cloud Synced Real-Time',

    // SMS & Notifications
    notifications: 'Notifications & SMS Alerts',
    simulatedSms: 'Simulated SMS on Farmer Mobile',
    markAllRead: 'Mark all as read',
    noNotifications: 'No alerts at the moment',

    // Admin
    adminDashboardTitle: 'APMC Mandi Operations & Real-Time Capacity Control',
    callNextToken: 'Call Next Token to Weighbridge',
    updateStep: 'Advance Processing Stage',
    mandiCapacity: 'Mandi Capacity & Slots',
    adjustSlots: 'Manage Daily Slot Capacity',
    mspManagement: 'Update MSP & State Bonus Rates',
    saveChanges: 'Save Changes',
  },
  hi: {
    appTitle: 'किसान सारथी म.प्र.',
    appSubtitle: 'स्मार्ट कृषि उपज मंडी उपार्जन एवं लाइव टोकन कतार प्रणाली',
    govtTag: 'मध्य प्रदेश शासन • किसान कल्याण एवं राज्य कृषि विपणन बोर्ड',
    tagline: 'शून्य प्रतीक्षा स्लॉट • पारदर्शी एमएसपी • त्वरित डीबीटी भुगतान',

    // Auth & Roles
    loginAsFarmer: 'किसान लॉगिन (Kisan Login)',
    loginAsAdmin: 'अधिकारी लॉगिन (मंडी सचिव / उपार्जन केंद्र)',
    loginDescription: 'ओटीपी आधारित सुरक्षित प्रमाणीकरण - किसानों व मंडी अधिकारियों के लिए',
    mobileNumber: 'मोबाइल नंबर / समग्र किसान आईडी',
    enterOtp: '4-अंकीय ओटीपी दर्ज करें',
    verifyOtp: 'सत्यापित करें और प्रवेश करें',
    resendOtp: 'पुनः ओटीपी भेजें',
    switchRole: 'भूमिका बदलें',
    adminKey: 'मंडी अधिकारी पासकोड',
    demoQuickLogin: 'त्वरित डेमो लॉगिन',
    asFarmer: 'किसान के रूप में लॉगिन करें (रमेश पटेल - सीहोर)',
    asAdmin: 'मंडी अधिकारी के रूप में लॉगिन करें (सीहोर मंडी)',
    logout: 'लॉगआउट',
    loggedInAs: 'सक्रिय उपयोगकर्ता',

    // Tabs
    tabActiveToken: 'मेरा लाइव टोकन',
    tabBookSlot: 'मंडी स्लॉट बुक करें',
    tabProfitCalc: 'उपज व लाभ कैलकुलेटर',
    tabMandiQueues: 'लाइव मंडी कतार स्थिति',
    tabMspRates: 'आज के समर्थन मूल्य (MSP)',
    tabWeather: 'मौसम व कटाई सलाह',
    tabAdminCapacity: 'मंडी क्षमता व कतार नियंत्रण',
    tabAdminProcurement: 'तौल पर्ची व डीबीटी भुगतान',
    tabAdminReports: 'राज्य स्तरीय रिपोर्ट',

    // Booking & Queue
    selectDistrict: 'जिला चुनें (मध्य प्रदेश)',
    selectMandi: 'कृषि उपज मंडी केंद्र चुनें',
    selectCrop: 'उपज / फसल चुनें',
    estimatedYield: 'कुल संभावित उपज (क्विंटल में)',
    acreage: 'खेत का रकबा (एकड़)',
    harvestDate: 'कटाई पूर्ण होने की संभावित तिथि',
    selectSlot: 'समय स्लॉट चुनें',
    vehicleType: 'परिवहन वाहन का प्रकार',
    vehicleNumber: 'वाहन / ट्रैक्टर नंबर',
    aiSlotRecommendation: 'एआई द्वारा अनुशंसित समय स्लॉट',
    aiSlotDesc: 'मंडी में भीड़, धर्मकांटा क्षमता व मौसम का विश्लेषण कर सबसे कम प्रतीक्षा वाला स्लॉट',
    recommendedSlotBadge: 'एआई अनुशंसित स्लॉट (न्यूनतम भीड़)',
    bookSlotBtn: 'पुष्टि करें और ई-टोकन प्राप्त करें',
    bookingSuccess: 'स्लॉट सफलतापूर्वक बुक हो गया!',
    tokenNumber: 'ई-टोकन क्रमांक',
    estimatedWait: 'अनुमानित प्रतीक्षा समय',
    tokensAhead: 'कतार में आपसे आगे टोकन',
    nowServing: 'वर्तमान में सेवा जारी टोकन',
    weighbridgesActive: 'सक्रिय तौल कांटे (धर्मकांटा)',
    gateStatus: 'मंडी गेट स्थिति',

    // Steps
    stepBooked: 'स्लॉट आरक्षित',
    stepGateEntered: 'गेट प्रवेश संपन्न',
    stepWeighbridgeGross: 'सकल वजन (धर्मकांटा 1)',
    stepQcInspection: 'गुणवत्ता व नमी जांच (QC)',
    stepUnloading: 'शेड में अनलोडिंग',
    stepWeighbridgeTare: 'खाली वजन (धर्मकांटा 2)',
    stepCompleted: 'जे-फॉर्म / तौल पर्ची जारी',

    // Payments
    dbtStatus: 'डीबीटी भुगतान स्थिति',
    payoutAmount: 'कुल उपार्जन राशि (MSP)',
    utrNumber: 'डीबीटी संदर्भ यूटीआर',
    bankAccount: 'आधार लिंक बैंक खाता',
    viewJForm: 'आधिकारिक जे-फॉर्म पर्ची देखें',
    printPass: 'गेट पास प्रिंट / डाउनलोड करें',

    // Profit Calc
    calculatorTitle: 'फसल उपज, समर्थन मूल्य आय एवं मुनाफा गणक',
    calculatorSubtitle: 'खेती की कुल लागत के मुकाबले सरकारी एमएसपी व राज्य बोनस से शुद्ध आय जानें',
    inputCostPerAcre: 'प्रति एकड़ लागत (बीज, खाद, कीटनाशक, डीजल, कटाई)',
    totalInputCost: 'कुल खेती लागत',
    grossRevenueMsp: 'एमएसपी पर कुल प्राप्ति',
    projectedNetProfit: 'अनुमानित शुद्ध मुनाफा',
    profitMargin: 'मुनाफा प्रतिशत',
    aiYieldAdvisorBtn: 'एआई उपज व मुनाफा सलाहकार चलाएं',

    // Offline
    offlineBadge: 'ऑफ़लाइन मोड (इंटरनेट बंद)',
    offlineDesc: 'जानकारी फोन में सुरक्षित है। इंटरनेट आते ही स्वतः सर्वर पर दर्ज होगी।',
    syncPendingCount: 'सिंक के लिए लंबित प्रविष्टियां',
    onlineSynced: 'क्लाउड सर्वर से पूर्णतः कनेक्टेड',

    // SMS & Notifications
    notifications: 'सूचनाएं एवं एसएमएस अलर्ट',
    simulatedSms: 'किसान के मोबाइल पर आया संदेश',
    markAllRead: 'सभी को पढ़ा हुआ चिन्हित करें',
    noNotifications: 'वर्तमान में कोई नया अलर्ट नहीं',

    // Admin
    adminDashboardTitle: 'मंडी संचालन, कतार प्रबंधन एवं क्षमता नियंत्रण',
    callNextToken: 'अगले टोकन को तौल कांटे पर बुलाएं',
    updateStep: 'प्रसंस्करण चरण आगे बढ़ाएं',
    mandiCapacity: 'मंडी क्षमता व स्लॉट',
    adjustSlots: 'दैनिक स्लॉट क्षमता तय करें',
    mspManagement: 'समर्थन मूल्य व राज्य बोनस अपडेट करें',
    saveChanges: 'परिवर्तन सहेजें',
  },
  mal: {
    appTitle: 'किसान सारथी (मालवा-म.प्र.)',
    appSubtitle: 'मंडी उपार्जन अर ऑनलाइन टोकन व्यवस्था',
    govtTag: 'मध्य प्रदेश सरकार • किसान कल्याण अर मंडी बोर्ड',
    tagline: 'नी कोई लंबी लाइन • पक्को समर्थन मूल्य • सीधो खातों म रुप्या',

    // Auth & Roles
    loginAsFarmer: 'किसान भाई लॉगिन',
    loginAsAdmin: 'मंडी साहेब लॉगिन',
    loginDescription: 'मोबाइल ओटीपी से तुरत-फुरत लॉगिन करो',
    mobileNumber: 'मोबाइल नंबर / समग्र किसान आईडी',
    enterOtp: '४ अंक को ओटीपी डालो',
    verifyOtp: 'सत्यापित करो अर भीतर आओ',
    resendOtp: 'वापस ओटीपी भेजो',
    switchRole: 'रोल बदलो',
    adminKey: 'अधिकारी पासकोड',
    demoQuickLogin: 'सीधो डेमो लॉगिन',
    asFarmer: 'किसान रमेश पटेल (सीहोर)',
    asAdmin: 'मंडी सचिव (सीहोर मंडी)',
    logout: 'बार निकलो',
    loggedInAs: 'लॉगिन है',

    // Tabs
    tabActiveToken: 'म्हारो लाइव टोकन',
    tabBookSlot: 'मंडी को स्लॉट काटो',
    tabProfitCalc: 'मुनाफो अर पैदावार देखो',
    tabMandiQueues: 'मंडी की भीड़-भाड़',
    tabMspRates: 'आज को सरकारी भाव',
    tabWeather: 'मौसम अर पानी की खबर',
    tabAdminCapacity: 'मंडी कांटे अर टोकन',
    tabAdminProcurement: 'तौल पर्ची अर रुप्या',
    tabAdminReports: 'पूरा म.प्र. की रिपोर्ट',

    // Booking & Queue
    selectDistrict: 'आपणो जिलो चुणो',
    selectMandi: 'मंडी केंद्र चुणो',
    selectCrop: 'जिन्स / फसल चुणो',
    estimatedYield: 'कतरो माल हे (क्विंटल म)',
    acreage: 'कतरो बीघा / एकड़ खेत हे',
    harvestDate: 'फसल कबाई पूरी वेगी',
    selectSlot: 'टेम को स्लॉट चुणो',
    vehicleType: 'ट्रैक्टर ट्रॉली / पिकअप',
    vehicleNumber: 'गाड़ी को नंबर',
    aiSlotRecommendation: 'एआई को सबसे चोखो स्लॉट',
    aiSlotDesc: 'कंप्यूटर ने जांच कर बतायो क कदे भीड़ नी रेवेगी',
    recommendedSlotBadge: 'कम भीड़ वालो स्लॉट (एआई)',
    bookSlotBtn: 'पक्को करो अर टोकन पर्ची लो',
    bookingSuccess: 'स्लॉट बुक वे गयो भाईसाब!',
    tokenNumber: 'टोकन नंबर',
    estimatedWait: 'कतरो टेम लागेगो',
    tokensAhead: 'आपसूं आगली गाड़ियां',
    nowServing: 'कांटा पे अबार नंबर',
    weighbridgesActive: 'चालू तौल कांटे',
    gateStatus: 'मंडी फाट की हालत',

    // Steps
    stepBooked: 'स्लॉट पक्को वे गयो',
    stepGateEntered: 'मंडी फाट म भीतर',
    stepWeighbridgeGross: 'भरेली गाड़ी को तौल',
    stepQcInspection: 'नमी अर दाणा की जांच',
    stepUnloading: 'माल ढल गयो',
    stepWeighbridgeTare: 'खाली गाड़ी को तौल',
    stepCompleted: 'तौल पर्ची निकळ गी',

    // Payments
    dbtStatus: 'खाता म रुप्या की स्थिति',
    payoutAmount: 'कुल रुप्या (एमएसपी)',
    utrNumber: 'बैंक को यूटीआर नंबर',
    bankAccount: 'जुड़ेड़ो बैंक खातो',
    viewJForm: 'सरकारी तौल पर्ची देखो',
    printPass: 'गेट पास डाउनलोड करो',

    // Profit Calc
    calculatorTitle: 'फसल पैदावार अर मुन्नाफो हिसाब',
    calculatorSubtitle: 'लागत काटो अर जानो कतरो नफो वेगो',
    inputCostPerAcre: 'एकड़ को खरचो (खाद-बीज-डीजल)',
    totalInputCost: 'कुल खेती खरचो',
    grossRevenueMsp: 'एमएसपी पे आवक',
    projectedNetProfit: 'बचेड़ो शुद्ध मुन्नाफो',
    profitMargin: 'नफो टको (%)',
    aiYieldAdvisorBtn: 'एआई सूं सलाह लो',

    // Offline
    offlineBadge: 'नेट नी चाल रियो (ऑफ़लाइन)',
    offlineDesc: 'चिंता मती करो, फोन म सब सेव हे। नेट आते ही सर्वर पे चढ़ जावेगो।',
    syncPendingCount: 'काम बाकी हे',
    onlineSynced: 'सब चोखो सर्वर पे सेव हे',

    // SMS & Notifications
    notifications: 'सूचना अर संदेश',
    simulatedSms: 'मोबाइल पे सरकारी एसएमएस',
    markAllRead: 'सब देख लीदा',
    noNotifications: 'अबार कोई संदेश नी हे',

    // Admin
    adminDashboardTitle: 'मंडी संचालन अर कांटे को काम',
    callNextToken: 'अगलो टोकन कांटे पे बुलाओ',
    updateStep: 'काम आगू बढ़ाओ',
    mandiCapacity: 'मंडी की क्षमता',
    adjustSlots: 'स्लॉट सेट करो',
    mspManagement: 'भाव अर बोनस बदलो',
    saveChanges: 'सहेज लो',
  }
};
