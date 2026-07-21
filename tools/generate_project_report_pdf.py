#!/usr/bin/env python3
"""Generate the polished UsedCarrot submission PDF from project evidence."""

from __future__ import annotations

from pathlib import Path
from xml.sax.saxutils import escape

from PIL import Image as PILImage
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    HRFlowable,
    Image,
    KeepTogether,
    LongTable,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
SCREENSHOTS = ROOT / "docs" / "screenshots"
TMP = ROOT / "tmp" / "pdfs"
CROPS = TMP / "crops"
OUTPUT = ROOT / "output" / "pdf" / "[WHS][secure-coding][01반]박도현(3206).pdf"

FONT_REGULAR = Path("/Users/dohyun/Library/Fonts/NanumGothic-Regular.ttf")
FONT_BOLD = Path("/Users/dohyun/Library/Fonts/NanumGothic-Bold.ttf")
FONT_EXTRA = Path("/Users/dohyun/Library/Fonts/NanumGothic-ExtraBold.ttf")

ORANGE = colors.HexColor("#FF6B12")
ORANGE_DARK = colors.HexColor("#D94E00")
ORANGE_PALE = colors.HexColor("#FFF2E8")
CREAM = colors.HexColor("#FFF9F4")
INK = colors.HexColor("#171717")
MUTED = colors.HexColor("#66605B")
LINE = colors.HexColor("#E8DED5")
GREEN = colors.HexColor("#0D8A5F")
RED = colors.HexColor("#B42318")
BLUE = colors.HexColor("#1769AA")


def register_fonts() -> None:
    pdfmetrics.registerFont(TTFont("Nanum", str(FONT_REGULAR)))
    pdfmetrics.registerFont(TTFont("NanumBold", str(FONT_BOLD)))
    pdfmetrics.registerFont(TTFont("NanumExtra", str(FONT_EXTRA)))
    pdfmetrics.registerFontFamily(
        "Nanum", normal="Nanum", bold="NanumBold", italic="Nanum", boldItalic="NanumBold"
    )


def crop_evidence() -> dict[str, Path]:
    """Trim oversized screenshot margins for PDF layout."""
    CROPS.mkdir(parents=True, exist_ok=True)

    boxes: dict[str, tuple[int, int, int, int]] = {
        "01-home.png": (0, 0, 1440, 1080),
        "02-login.png": (180, 35, 1260, 860),
        "03-register.png": (180, 25, 1260, 930),
        "04-products-list.png": (0, 0, 1440, 850),
        "05-product-detail.png": (0, 0, 1440, 860),
        "06-home-logged-in.png": (0, 0, 1440, 1080),
        "07-product-detail-buyer.png": (0, 0, 1440, 860),
        "08-chat-room.png": (0, 0, 1440, 830),
        "09-chat-list.png": (0, 0, 1440, 650),
        "10-mypage.png": (70, 0, 796, 1440),
        "11-wallet.png": (0, 0, 1440, 590),
        "12-favorites.png": (0, 0, 1440, 500),
        "13-report-form.png": (0, 0, 1440, 690),
        "14-product-new.png": (80, 0, 1360, 980),
        "15-my-products.png": (0, 0, 1440, 720),
        "17-admin-dashboard.png": (0, 0, 1440, 560),
        "18-admin-users.png": (0, 0, 1440, 720),
        "19-admin-products.png": (0, 0, 1440, 420),
        "20-admin-reports.png": (0, 0, 1440, 520),
        "21-admin-wallet-tx.png": (0, 0, 1440, 500),
        "22-admin-audit-logs.png": (40, 0, 1030, 1440),
        "manual/m01-wallet-already-linked-error.png": (0, 0, 1400, 720),
        "manual/m02-seller-buyer-wallets-sidebyside.png": (0, 45, 1600, 880),
        "manual/m03-chat-two-sessions.png": (0, 45, 1600, 880),
        "manual/m04-product-metamask-pay-button.png": (0, 0, 1600, 1320),
        "manual/m05-metamask-connect-prompt.png": (0, 45, 1600, 810),
        "manual/m06-metamask-tx.png": (0, 0, 786, 1226),
        "manual/m07-metamask-confirm.png": (0, 0, 786, 1196),
        "manual/m08-after-pay.png": (0, 0, 790, 1572),
    }

    results: dict[str, Path] = {}
    for rel, box in boxes.items():
        source = SCREENSHOTS / rel
        if not source.exists():
            raise FileNotFoundError(source)
        with PILImage.open(source) as im:
            x0, y0, x1, y1 = box
            x1, y1 = min(x1, im.width), min(y1, im.height)
            cropped = im.convert("RGB").crop((x0, y0, x1, y1))
            target = CROPS / rel.replace("/", "__")
            cropped.save(target, optimize=True)
            results[rel] = target
    return results


class SectionMarker(Flowable):
    def __init__(self, number: str, label: str):
        super().__init__()
        self.number = number
        self.label = label
        self.width = 170 * mm
        self.height = 14 * mm

    def draw(self):
        c = self.canv
        c.setFillColor(ORANGE)
        c.roundRect(0, 1.5 * mm, 11 * mm, 11 * mm, 2.5 * mm, fill=1, stroke=0)
        c.setFillColor(colors.white)
        c.setFont("NanumBold", 10)
        c.drawCentredString(5.5 * mm, 5.2 * mm, self.number)
        c.setFillColor(MUTED)
        c.setFont("NanumBold", 8.5)
        c.drawString(15 * mm, 5.2 * mm, self.label.upper())
        c.setStrokeColor(LINE)
        c.line(15 * mm, 2.5 * mm, self.width, 2.5 * mm)


class ArchitectureDiagram(Flowable):
    def __init__(self):
        super().__init__()
        self.width = 170 * mm
        self.height = 68 * mm

    def draw(self):
        c = self.canv
        c.setFillColor(CREAM)
        c.roundRect(0, 0, self.width, self.height, 4 * mm, fill=1, stroke=0)
        nodes = [
            (5, 40, 29, 17, "Browser", "+ MetaMask", ORANGE_PALE, ORANGE_DARK),
            (42, 40, 34, 17, "Spring MVC", "+ Thymeleaf", colors.white, INK),
            (84, 40, 30, 17, "Service", "security boundary", colors.white, INK),
            (122, 48, 42, 14, "JPA / H2", "data", colors.white, INK),
            (122, 28, 42, 14, "Web3j / RPC", "Sepolia", colors.white, INK),
            (84, 8, 30, 14, "AuditLogger", "masked logs", colors.white, INK),
        ]
        for x, y, w, h, title, sub, fill, stroke in nodes:
            c.setFillColor(fill)
            c.setStrokeColor(stroke)
            c.setLineWidth(0.8)
            c.roundRect(x * mm, y * mm, w * mm, h * mm, 2.2 * mm, fill=1, stroke=1)
            c.setFillColor(INK)
            c.setFont("NanumBold", 8.5)
            c.drawCentredString((x + w / 2) * mm, (y + h - 6) * mm, title)
            c.setFillColor(MUTED)
            c.setFont("Nanum", 6.8)
            c.drawCentredString((x + w / 2) * mm, (y + 4) * mm, sub)
        arrows = [((34, 48.5), (42, 48.5)), ((76, 48.5), (84, 48.5)), ((114, 50), (122, 55)), ((114, 45), (122, 35)), ((99, 40), (99, 22))]
        c.setStrokeColor(ORANGE_DARK)
        c.setFillColor(ORANGE_DARK)
        c.setLineWidth(1.2)
        for (x1, y1), (x2, y2) in arrows:
            c.line(x1 * mm, y1 * mm, x2 * mm, y2 * mm)
            c.circle(x2 * mm, y2 * mm, 0.7 * mm, fill=1, stroke=0)


class PaymentSequence(Flowable):
    def __init__(self):
        super().__init__()
        self.width = 170 * mm
        self.height = 72 * mm

    def draw(self):
        c = self.canv
        steps = [
            ("1", "지갑 연결", "personal_sign\nnonce 1회 사용"),
            ("2", "상품 등록", "ETH→wei\npayToAddress 고정"),
            ("3", "MetaMask 전송", "상품 ID 메모\ntxHash 반환"),
            ("4", "서버 검증", "chain/from/to/value\n3 confirmations"),
            ("5", "거래 확정", "SOLD 전환\ntx hash 기록"),
        ]
        gap = 3.5 * mm
        w = (self.width - gap * 4) / 5
        for i, (num, title, sub) in enumerate(steps):
            x = i * (w + gap)
            c.setFillColor(colors.white)
            c.setStrokeColor(LINE)
            c.roundRect(x, 8 * mm, w, 54 * mm, 3 * mm, fill=1, stroke=1)
            c.setFillColor(ORANGE)
            c.circle(x + w / 2, 53 * mm, 5 * mm, fill=1, stroke=0)
            c.setFillColor(colors.white)
            c.setFont("NanumBold", 9)
            c.drawCentredString(x + w / 2, 50.2 * mm, num)
            c.setFillColor(INK)
            c.setFont("NanumBold", 8.2)
            c.drawCentredString(x + w / 2, 39 * mm, title)
            c.setFillColor(MUTED)
            c.setFont("Nanum", 6.7)
            for j, line in enumerate(sub.split("\n")):
                c.drawCentredString(x + w / 2, (28 - j * 5) * mm, line)


def styles():
    base = getSampleStyleSheet()
    return {
        "cover_kicker": ParagraphStyle("cover_kicker", parent=base["Normal"], fontName="NanumBold", fontSize=10, leading=14, textColor=ORANGE_DARK, spaceAfter=8),
        "cover_title": ParagraphStyle("cover_title", parent=base["Title"], fontName="NanumExtra", fontSize=34, leading=42, textColor=INK, spaceAfter=12),
        "cover_sub": ParagraphStyle("cover_sub", parent=base["Normal"], fontName="Nanum", fontSize=13, leading=20, textColor=MUTED, spaceAfter=24),
        "h1": ParagraphStyle("h1", parent=base["Heading1"], fontName="NanumExtra", fontSize=22, leading=29, textColor=INK, spaceBefore=3, spaceAfter=10, keepWithNext=True),
        "h2": ParagraphStyle("h2", parent=base["Heading2"], fontName="NanumBold", fontSize=14, leading=20, textColor=INK, spaceBefore=9, spaceAfter=6, keepWithNext=True),
        "body": ParagraphStyle("body", parent=base["BodyText"], fontName="Nanum", fontSize=9.5, leading=16, textColor=INK, spaceAfter=6, wordWrap="CJK"),
        "body_small": ParagraphStyle("body_small", parent=base["BodyText"], fontName="Nanum", fontSize=8, leading=12, textColor=INK, wordWrap="CJK"),
        "caption": ParagraphStyle("caption", parent=base["Normal"], fontName="Nanum", fontSize=7.5, leading=11, textColor=MUTED, alignment=TA_CENTER, spaceBefore=3, spaceAfter=5),
        "callout": ParagraphStyle("callout", parent=base["BodyText"], fontName="NanumBold", fontSize=10, leading=17, textColor=ORANGE_DARK, backColor=ORANGE_PALE, borderPadding=(9, 11, 9, 11), borderColor=ORANGE_PALE, borderRadius=4, spaceBefore=4, spaceAfter=10),
        "code": ParagraphStyle("code", parent=base["Code"], fontName="Nanum", fontSize=7.5, leading=12, textColor=colors.HexColor("#F5F5F5"), backColor=colors.HexColor("#242424"), borderPadding=8, spaceAfter=8),
        "toc": ParagraphStyle("toc", parent=base["Normal"], fontName="NanumBold", fontSize=10, leading=18, textColor=INK),
        "tiny": ParagraphStyle("tiny", parent=base["Normal"], fontName="Nanum", fontSize=6.8, leading=9.5, textColor=MUTED, wordWrap="CJK"),
    }


S = {}


def p(text: str, style: str = "body") -> Paragraph:
    return Paragraph(text, S[style])


def h1(number: str, title: str, label: str) -> list[Flowable]:
    return [SectionMarker(number, label), p(f"{number}. {title}", "h1")]


def h2(title: str) -> Paragraph:
    return p(title, "h2")


def bullet(text: str) -> Paragraph:
    return Paragraph(f"•  {text}", ParagraphStyle("bullet", parent=S["body"], leftIndent=5 * mm, firstLineIndent=-4 * mm, spaceAfter=3))


def styled_table(rows, widths, header=True, font_size=7.5, aligns=None):
    data = []
    for ridx, row in enumerate(rows):
        style = S["body_small"] if ridx else ParagraphStyle("th", parent=S["body_small"], fontName="NanumBold", textColor=colors.white)
        data.append([cell if isinstance(cell, Flowable) else Paragraph(str(cell), style) for cell in row])
    t = LongTable(data, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), ORANGE_DARK if header else CREAM),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white if header else INK),
        ("FONTNAME", (0, 0), (-1, 0), "NanumBold"),
        ("FONTNAME", (0, 1), (-1, -1), "Nanum"),
        ("FONTSIZE", (0, 0), (-1, -1), font_size),
        ("LEADING", (0, 0), (-1, -1), font_size + 4),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]
    for row in range(1, len(data)):
        if row % 2 == 0:
            commands.append(("BACKGROUND", (0, row), (-1, row), CREAM))
    if aligns:
        for col, align in enumerate(aligns):
            commands.append(("ALIGN", (col, 0), (col, -1), align))
    t.setStyle(TableStyle(commands))
    return t


def image_flow(path: Path, max_w=170 * mm, max_h=105 * mm) -> Image:
    with PILImage.open(path) as im:
        iw, ih = im.size
    ratio = min(max_w / iw, max_h / ih)
    return Image(str(path), width=iw * ratio, height=ih * ratio, hAlign="CENTER")


def figure(path: Path, caption: str, max_w=170 * mm, max_h=105 * mm):
    return KeepTogether([image_flow(path, max_w, max_h), p(caption, "caption")])


def paired_figures(left: tuple[Path, str], right: tuple[Path, str], max_h=68 * mm):
    cells = []
    for path, caption in (left, right):
        cells.append([image_flow(path, 80 * mm, max_h), p(caption, "caption")])
    t = Table([[cells[0], cells[1]]], colWidths=[84 * mm, 84 * mm], hAlign="CENTER")
    t.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 2), ("RIGHTPADDING", (0, 0), (-1, -1), 2)]))
    return t


def on_page(canvas, doc):
    page = canvas.getPageNumber()
    canvas.saveState()
    if page > 1:
        canvas.setStrokeColor(LINE)
        canvas.line(20 * mm, A4[1] - 15 * mm, A4[0] - 20 * mm, A4[1] - 15 * mm)
        canvas.setFont("NanumBold", 7.2)
        canvas.setFillColor(ORANGE_DARK)
        canvas.drawString(20 * mm, A4[1] - 11.5 * mm, "USEDCARROT")
        canvas.setFont("Nanum", 7.2)
        canvas.setFillColor(MUTED)
        canvas.drawRightString(A4[0] - 20 * mm, A4[1] - 11.5 * mm, "Secure Coding Project Report")
    canvas.setFont("Nanum", 7)
    canvas.setFillColor(MUTED)
    canvas.drawString(20 * mm, 11 * mm, "박도현 · WHS 4기 01반")
    canvas.drawRightString(A4[0] - 20 * mm, 11 * mm, f"{page:02d}")
    canvas.restoreState()


def build_story(crops: dict[str, Path]) -> list[Flowable]:
    f = lambda rel: crops[rel]
    story: list[Flowable] = []

    # Cover — title + personal info only
    story += [
        Spacer(1, 48 * mm),
        p("UsedCarrot 개발 보고서", "cover_title"),
        p("Tiny Second-hand Shopping Platform", "cover_sub"),
        HRFlowable(width="100%", thickness=1.5, color=ORANGE, spaceBefore=2, spaceAfter=18),
    ]
    cover_rows = [
        ["이름", "박도현"],
        ["소속", "화이트햇스쿨 4기 01반"],
        ["연락처 뒷자리", "3206"],
        ["GitHub", "github.com/no-carve-only-pizza/usedcarrot"],
    ]
    story.append(styled_table([["항목", "내용"], *cover_rows], [40 * mm, 130 * mm], font_size=10))
    story.append(PageBreak())

    # Contents / summary
    story += h1("0", "보고서 구성", "CONTENTS")
    toc = [
        ("01", "과제 요약", "목적 · 기술 스택 · 핵심 설계"),
        ("02", "요구사항 분석", "최소 요구 대응 · 범위 조정"),
        ("03", "시스템 설계", "아키텍처 · 결제 흐름 · 권한 · 데이터"),
        ("04", "구현 및 화면", "인증 · 지갑 · 상품 · 채팅 · 관리자"),
        ("05", "시큐어코딩", "인증 · 인가 · 입력 · 업로드 · 결제"),
        ("06", "테스트", "자동 테스트 · 실기 증거"),
        ("07", "발견 약점과 수정", "위험도와 보완 결과"),
        ("08–09", "실행 및 유지보수", "재현 방법 · 운영 전환 계획"),
    ]
    for num, title, sub in toc:
        story.append(Table([[p(num, "cover_kicker"), p(title, "toc"), p(sub, "body_small")]], colWidths=[18 * mm, 43 * mm, 109 * mm], style=TableStyle([("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("LINEBELOW", (0, 0), (-1, -1), 0.5, LINE), ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7)])))
    story += [Spacer(1, 8 * mm), p("핵심 결론", "h2"), p("UsedCarrot는 서버가 가상 잔액을 보관하는 구조 대신, MetaMask가 Sepolia ETH를 판매자에게 직접 전송하고 서버가 영수증을 검증하는 구조를 선택했다. 따라서 상품 ID, 구매자·판매자 주소, 금액, 컨펌 수, tx hash 재사용을 서버에서 함께 검증하는 것이 가장 중요한 보안 경계다.", "callout"), PageBreak()]

    # 1
    story += h1("1", "과제 요약", "OVERVIEW")
    story += [h2("1.1 목적"), p("중고거래 플랫폼의 최소 요구사항을 만족하는 웹 애플리케이션을 만들고, <b>요구사항 분석 → 시스템 설계 → 구현 → 체크리스트/테스트 → 유지보수</b> 전 과정을 기록한다. 개발 중 발견한 보안 약점과 수정 내용도 함께 정리한다."), p("AI 도구를 활용해 구현과 검토 속도를 높였지만, 결제·인가·파일 업로드처럼 보안에 직접 영향을 주는 경계는 코드와 실제 화면을 다시 확인했다.")]
    stack = [["구분", "기술"], ["Backend", "Java 21, Spring Boot 3, Gradle"], ["View", "Thymeleaf 서버 렌더링"], ["Security", "Spring Security, BCrypt, CSRF, 세션"], ["Persistence", "Spring Data JPA, H2"], ["결제", "Ethereum Sepolia, MetaMask, Web3j RPC 검증"], ["Test", "JUnit 5, Spring Boot Test, MockMvc"]]
    story += [h2("1.2 기술 스택"), styled_table(stack, [33 * mm, 137 * mm], font_size=8.2)]
    decisions = [["결정", "내용"], ["결제", "MetaMask로 판매자 주소에 Sepolia ETH 전송 → 서버 영수증 검증 후 SOLD"], ["서버 역할", "ETH를 보관하지 않음. wallets / wallet_transactions는 결제 기록·감사용"], ["채팅", "전체 공개 채팅 대신 상품별 1:1만 구현"], ["H2 콘솔", "기본 비활성"]]
    story += [h2("1.3 핵심 설계 결정"), p("슬라이드의 ‘유저 간 송금’을 DB 포인트가 아니라 <b>테스트넷 ETH 실제 전송</b>으로 구현했다."), styled_table(decisions, [33 * mm, 137 * mm], font_size=8.0), PageBreak()]

    # 2
    story += h1("2", "요구사항 분석", "REQUIREMENTS")
    reqs = [["슬라이드 요구", "구현", "패키지/경로"], ["가입·로그인·마이페이지", "이메일 가입, 세션 로그인, 프로필·비번·지갑·탈퇴", "auth, user"], ["상품 등록·조회·관리", "CRUD, 이미지, 내 상품, 검색·필터", "product"], ["사용자 소통", "상품별 1:1 채팅", "chat"], ["악성 유저·상품 차단", "신고 + 관리자 승인 후 누적 제재", "report"], ["유저 간 송금", "Sepolia ETH + MetaMask", "wallet, crypto"], ["상품 검색", "키워드·카테고리·지역·상태", "/products"], ["관리자", "사용자·상품·신고·결제내역·감사로그", "/admin/**"]]
    story += [h2("2.1 최소 요구사항 대응"), styled_table(reqs, [43 * mm, 91 * mm, 36 * mm], font_size=7.5)]
    story += [h2("2.2 범위 조정 근거"), bullet("<b>전체 채팅 제외:</b> 핵심은 거래 상대와의 소통이며, 전체 채팅은 스팸·악용 위험이 크다."), bullet("<b>온체인 결제 채택:</b> 모의 잔액 대신 체인 위 전송을 사용해 서버 잔액 조작과 다계정 인플레이션 문제를 구조적으로 줄였다."), bullet("<b>신고 ≠ 즉시 제재:</b> 허위 신고로 자동 숨김·정지가 되지 않도록 관리자 RESOLVED만 누적한다."), figure(f("01-home.png"), "그림 1. 비로그인 홈 — 공개 상품 탐색과 핵심 진입점", max_h=78 * mm), PageBreak()]

    # 3
    story += h1("3", "시스템 설계", "SYSTEM DESIGN")
    story += [h2("3.1 아키텍처"), ArchitectureDiagram(), p("컨트롤러는 화면과 입력을 받고, <b>권한·가격·결제 검증은 서비스 계층</b>에서 수행한다. 구매 금액은 클라이언트 입력이 아니라 서버에 저장된 wei 가격과 온체인 value를 대조한다."), h2("3.2 온체인 결제 시퀀스"), PaymentSequence(), p("서버 검증 항목: chainId=Sepolia, receipt success, confirmations ≥ 3, from=구매자 지갑, to=상품 payToAddress, value ≥ 상품 가격, input=해당 상품 PaymentMemo.", "callout"), PageBreak()]
    screens = [["화면", "경로", "권한"], ["홈·목록·상세", "/, /products, /products/{id}", "공개"], ["등록·수정", "/products/new, .../edit", "회원 / 소유자"], ["채팅", "/chat, /chat/{id}", "참여자"], ["결제 내역", "/wallet", "본인"], ["신고", "/reports/new", "회원"], ["마이페이지", "/users/me", "본인"], ["관리자", "/admin/**", "ROLE_ADMIN"]]
    data = [["테이블", "역할", "보안 포인트"], ["users", "계정, wallet_address, locked_until", "password_hash만 저장, email/nickname unique"], ["products", "상품, price(wei), pay_to_address", "가격·지갑 고정, 상태 기반 노출"], ["product_images", "이미지 메타", "UUID 저장명, 상태 확인 후 제공"], ["chat_rooms / messages", "1:1 채팅", "참여자만 조회·전송"], ["wallets / transactions", "결제 원장", "tx hash unique, 잔액 포인트 아님"], ["reports", "신고", "24h 중복 제한, 승인 후 제재"], ["audit_logs", "감사", "민감 키워드 마스킹"]]
    story += h1("3", "권한과 데이터 경계", "ACCESS & DATA")
    story += [h2("3.3 주요 화면·권한"), styled_table(screens, [45 * mm, 85 * mm, 40 * mm], font_size=7.6), h2("3.4 데이터 모델 — 보안 관점"), styled_table(data, [40 * mm, 59 * mm, 71 * mm], font_size=7.2), PageBreak()]

    # 4 implementation
    story += h1("4", "구현 및 화면", "IMPLEMENTATION")
    story += [h2("4.1 홈·인증"), p("비로그인 상태에서도 홈·목록·상세를 볼 수 있다. 로그인 후에는 내 상품·채팅·결제·마이페이지가 내비게이션에 추가된다."), paired_figures((f("02-login.png"), "그림 2. 로그인"), (f("03-register.png"), "그림 3. 회원가입"), 62 * mm), figure(f("06-home-logged-in.png"), "그림 4. 로그인 후 홈 — 회원 기능 내비게이션 노출", max_h=82 * mm), PageBreak()]
    story += h1("4", "사용자·지갑 연결", "WALLET LINKING")
    story += [p("회원가입 시 결제 원장만 생성하고 MetaMask 주소는 비워 둔다. 마이페이지에서 세션 nonce 기반 서명을 검증해 주소를 연결하며, 이미 다른 계정에 묶인 주소는 거부한다."), figure(f("manual/m01-wallet-already-linked-error.png"), "그림 5. 이미 연결된 지갑 주소 재사용 거부", max_h=76 * mm), figure(f("manual/m02-seller-buyer-wallets-sidebyside.png"), "그림 6. 판매자·구매자 각각 Sepolia 지갑 연결", max_h=78 * mm), PageBreak()]
    story += h1("4", "마이페이지와 상품", "USER & PRODUCT")
    story += [paired_figures((f("10-mypage.png"), "그림 7. 마이페이지 전체"), (f("14-product-new.png"), "그림 8. 상품 등록"), 94 * mm), p("카테고리는 화이트리스트로 제한하고 가격은 ETH에서 wei로 변환한다. 이미지는 JPG/PNG만 허용하며, 판매자가 바꿀 수 있는 상태는 ON_SALE과 RESERVED뿐이다. SOLD는 온체인 결제 확정 시에만 설정된다."), PageBreak()]
    story += h1("4", "상품 탐색과 상세", "PRODUCT EXPERIENCE")
    story += [paired_figures((f("04-products-list.png"), "그림 9. 검색·필터가 있는 상품 목록"), (f("05-product-detail.png"), "그림 10. 공개 상품 상세"), 68 * mm), paired_figures((f("15-my-products.png"), "그림 11. 판매자의 내 상품"), (f("07-product-detail-buyer.png"), "그림 12. 구매자 상세 — 관심·문의·신고"), 68 * mm), PageBreak()]
    story += h1("4", "MetaMask 결제 진입", "ON-CHAIN PAYMENT")
    story += [p("예약 상품 상세에는 판매자 지갑 주소와 ‘MetaMask로 ETH 결제’ 버튼을 표시한다. 결제 버튼은 클라이언트 지갑 UI를 열고, 서버는 전송 후 받은 txHash를 별도로 검증한다."), figure(f("manual/m04-product-metamask-pay-button.png"), "그림 13. 0.0008 ETH 예약 상품과 MetaMask 결제 버튼", max_h=167 * mm), PageBreak()]
    story += h1("4", "상품별 1:1 채팅", "CHAT")
    story += [p("채팅방은 구매자와 판매자만 조회·전송할 수 있다. 거래 완료·숨김·삭제 상품에서는 신규 전송을 차단한다."), figure(f("manual/m03-chat-two-sessions.png"), "그림 14. 일반 창과 시크릿 창의 구매자·판매자 동시 대화", max_h=87 * mm), paired_figures((f("08-chat-room.png"), "그림 15. 단일 채팅방"), (f("09-chat-list.png"), "그림 16. 채팅 목록"), 72 * mm), PageBreak()]
    story += h1("4", "MetaMask 전송 흐름", "METAMASK")
    story += [figure(f("manual/m05-metamask-connect-prompt.png"), "그림 17. localhost:8081 MetaMask 연결 요청", max_h=82 * mm), paired_figures((f("manual/m06-metamask-tx.png"), "그림 18. 연결할 계정 선택"), (f("manual/m07-metamask-confirm.png"), "그림 19. SepoliaETH 0.0008 전송 확인"), 98 * mm), PageBreak()]
    story += h1("4", "결제 기록", "PAYMENT RECORD")
    story += [paired_figures((f("manual/m08-after-pay.png"), "그림 20. 전송 진행 화면"), (f("11-wallet.png"), "그림 21. 서비스 결제 내역"), 100 * mm), p("서버는 txHash를 받은 뒤 RPC로 영수증을 검증한다. 공개 RPC에 의존하지 않는 단위 테스트에서는 지갑 미연결 상품 등록과 알 수 없는 tx 확정 요청이 거부되는지 확인한다."), PageBreak()]
    story += h1("4", "신고와 관심", "REPORTING")
    report_rules = [["대상", "승인 신고", "처리"], ["상품", "3회", "HIDDEN"], ["사용자", "5회 / 10회", "LIMITED / SUSPENDED"]]
    story += [p("본인·자기 신고와 24시간 내 중복 신고를 차단한다. 제재 카운트는 관리자 승인 후에만 누적한다."), styled_table(report_rules, [48 * mm, 48 * mm, 74 * mm], font_size=8.2), paired_figures((f("13-report-form.png"), "그림 22. 신고 폼"), (f("12-favorites.png"), "그림 23. 관심 상품"), 73 * mm), PageBreak()]
    story += h1("4", "관리자 기능", "ADMINISTRATION")
    story += [paired_figures((f("17-admin-dashboard.png"), "그림 24. 관리자 대시보드"), (f("18-admin-users.png"), "그림 25. 사용자 관리"), 67 * mm), paired_figures((f("20-admin-reports.png"), "그림 26. 신고 관리"), (f("21-admin-wallet-tx.png"), "그림 27. 지갑 거래 내역"), 67 * mm), p("관리자 기능은 ROLE_ADMIN으로 제한한다. 일반 사용자의 /admin 접근은 403으로 거부한다."), PageBreak()]
    story += h1("4", "관리자 감사 로그", "ADMIN EVIDENCE")
    story += [p("로그인·로그아웃·지갑 연결·메시지 전송 같은 주요 이벤트를 관리자 화면에서 확인한다. 상세 문자열에는 비밀번호·토큰·세션·secret을 남기지 않고, 지갑 주소처럼 감사에 필요한 식별 정보만 기록한다."), figure(f("22-admin-audit-logs.png"), "그림 28. 관리자 감사 로그", max_h=157 * mm), PageBreak()]

    # 5 security
    story += h1("5", "시큐어코딩", "SECURE CODING")
    security = [["영역", "적용 내용"], ["인증·세션", "BCrypt, 메시지 일반화, 5회 실패 시 15분 잠금, IP rate limit, 로그인 시 세션 재발급, CSRF, HttpOnly/SameSite, CSP"], ["인가", "상품은 소유자/관리자, 채팅은 참여자, 결제내역은 본인, /admin/**는 ROLE_ADMIN"], ["입력·XSS·SQLi", "Bean Validation, enum/화이트리스트, Thymeleaf th:text, JPA 파라미터 바인딩"], ["파일 업로드", "JPG/PNG, 확장자·MIME·시그니처·ImageIO·5MB·5개, UUID 파일명, 숨김/삭제 이미지 URL 차단"], ["로그", "password/token/session/secret 마스킹, 신뢰 프록시가 아니면 X-Forwarded-For 무시"]]
    story += [styled_table(security, [38 * mm, 132 * mm], font_size=7.8), h2("5.1 온체인 결제 위협 모델")]
    threats = [["위협", "대응"], ["예전 tx로 다른 상품 SOLD", "PaymentMemo로 productId 바인딩"], ["판매중 가격·지갑 변경(rug)", "payToAddress 고정, 판매중 가격·지갑 재연결 제한"], ["서명 재사용", "세션 nonce + TTL + 1회 소모"], ["미확정 tx", "confirmations ≥ 3, receipt success"], ["tx hash 재사용", "동일 구매자·상품에만 멱등 반환"]]
    story += [styled_table(threats, [67 * mm, 103 * mm], font_size=8.0), p("관련 문서: docs/SECURE_CODING_CHECKLIST.md · docs/SECURITY_HARDENING.md", "caption"), PageBreak()]

    # 6 tests
    story += h1("6", "테스트", "VERIFICATION")
    story += [h2("6.1 자동 테스트"), p("<font color='#FFFFFF'>./gradlew test&nbsp;&nbsp;&nbsp;&nbsp;→&nbsp;&nbsp;&nbsp;&nbsp;BUILD SUCCESSFUL</font>", "code")]
    tests = [["테스트", "결과"], ["가입 시 원장 0·지갑 미연결", "통과"], ["지갑 없이 상품 등록", "거부"], ["잘못된 tx hash 확정", "거부, 상태 유지"], ["LIMITED 판매자 수정", "거부"], ["미승인 신고 3회", "상품 숨김 안 됨"], ["IP rate limit / 위조 헤더", "통과"], ["위장 PNG 업로드", "거부"], ["/products/new 비로그인 / 없는 상품", "리다이렉트 / 404"]]
    story += [styled_table(tests, [126 * mm, 44 * mm], font_size=8.0), h2("6.2 수동·실기 확인")]
    manual = [["ID", "내용", "증거"], ["TC-UI-001", "홈·목록·상세", "01, 04, 05"], ["TC-AUTH-001", "로그인·가입", "02, 03"], ["TC-WAL-001", "지갑 연결·중복 거부", "m01, m02"], ["TC-CHAT-001", "양측 1:1 채팅", "m03"], ["TC-PAY-001", "MetaMask 연결·0.0008 ETH 전송 UI", "m04–m08"], ["TC-ADM-001", "관리자 메뉴", "17–22"], ["TC-ADM-002", "일반 사용자 /admin", "403"]]
    story += [styled_table(manual, [35 * mm, 97 * mm, 38 * mm], font_size=7.8), PageBreak()]

    # 7
    story += h1("7", "발견한 보안 약점과 수정", "SECURITY FIX LOG")
    fixes = [["약점", "위험", "수정"], ["H2 콘솔 공개", "높음", "기본 비활성"], ["상품 소유자 미검증 / edit 노출", "높음", "GET·POST 모두 검증"], ["업로드 확장자만 검증", "중간", "MIME·시그니처·디코딩"], ["숨김 상품 이미지 URL", "중간", "상태 확인 컨트롤러"], ["허위 신고 자동 제재", "높음", "관리자 승인만 누적"], ["공개 기본 관리자", "치명", "환경변수로만 생성"], ["tx↔상품 미바인딩", "높음", "PaymentMemo"], ["판매자 rug", "높음", "payToAddress·가격 고정"], ["지갑 서명 재사용", "중간", "nonce"], ["XFF로 감사 IP 위조", "중간", "신뢰 프록시만"], ["무차별 대입", "중간", "계정 잠금 + IP limit"]]
    story += [p("구현 과정에서 발견한 약점은 위험도와 공격 경로를 기준으로 정리하고, 서비스 계층 검증·기본 설정 변경·입력 검증 강화로 보완했다."), styled_table(fixes, [70 * mm, 25 * mm, 75 * mm], font_size=7.7), Spacer(1, 5 * mm), p("상세 기록: docs/SECURITY_FIX_LOG.md · docs/SECURITY_HARDENING.md", "callout"), PageBreak()]

    # 8–9
    story += h1("8", "실행 방법", "RUNBOOK")
    code = "git clone https://github.com/no-carve-only-pizza/usedcarrot.git<br/>cd usedcarrot<br/>export USEDCARROT_ADMIN_EMAIL='admin@example.com'<br/>export USEDCARROT_ADMIN_PASSWORD='ChangeThisAdmin1!'<br/>./gradlew bootRun --args='--spring.profiles.active=local'"
    story += [p(code, "code"), bullet("앱: http://localhost:8081"), bullet("선택 환경변수: USEDCARROT_ETH_RPC_URL"), bullet("테스트: ./gradlew test"), bullet("local 데모 계정은 seller@demo.local / buyer@demo.local이며 MetaMask 연결이 필요하다."), p("관리자 계정은 소스에 하드코딩하지 않고 환경변수로만 생성한다.", "callout")]
    story += [h2("9. 유지보수"), bullet("Spring과 의존성 보안 패치를 주기적으로 적용한다."), bullet("공개 RPC를 자체 또는 유료 RPC로 전환하고 컨펌 수를 운영 환경에 맞게 조정한다."), bullet("신고 정책과 관리자 2인 승인, 채팅 읽음, 이미지 썸네일을 검토한다."), bullet("운영 시 H2→MySQL, HTTPS, 관리자 비밀번호 교체가 필요하다."), bullet("메인넷·실결제 전환 전 키 관리와 법무 검토가 필요하다. 현재는 Sepolia 교육용이다."), Spacer(1, 8 * mm), p("UsedCarrot는 기능 구현을 넘어 ‘누가, 어떤 값을, 어느 시점에 신뢰할 수 있는가’를 기준으로 서버의 보안 경계를 설계했다. 특히 온체인 결제는 브라우저 결과를 그대로 신뢰하지 않고 RPC 영수증을 재검증하는 구조로 완성했다.", "callout"), PageBreak()]

    # appendix
    story += [SectionMarker("A", "APPENDIX"), p("부록. 캡처 인덱스", "h1")]
    appendix = [["파일", "화면"], ["01–03", "홈, 로그인, 회원가입"], ["04–07, 14–16", "상품 목록·상세·등록·내 상품·수정"], ["08–09", "채팅"], ["10–13", "마이페이지, 결제, 관심, 신고"], ["17–22", "관리자"], ["manual m01", "지갑 중복 연결 오류"], ["manual m02", "판매자·구매자 지갑 연결"], ["manual m03", "1:1 채팅 양측"], ["manual m04", "상품 상세 MetaMask 결제 버튼"], ["manual m05–m08", "MetaMask 연결·전송 UI"]]
    story += [styled_table(appendix, [46 * mm, 124 * mm], font_size=8.2)]
    return story


def main() -> None:
    global S
    register_fonts()
    S = styles()
    TMP.mkdir(parents=True, exist_ok=True)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    crops = crop_evidence()

    doc = BaseDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        leftMargin=20 * mm,
        rightMargin=20 * mm,
        topMargin=21 * mm,
        bottomMargin=18 * mm,
        title="UsedCarrot 개발 보고서",
        author="박도현",
        subject="WHS Secure Coding Project Report",
    )
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="normal")
    doc.addPageTemplates([PageTemplate(id="report", frames=[frame], onPage=on_page)])
    doc.build(build_story(crops))
    print(OUTPUT)


if __name__ == "__main__":
    main()
