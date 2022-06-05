import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function LanguageKatakana(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-language-katakana",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 5h6.586a1 1 0 0 1 .707 1.707l-1.293 1.293"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 8c0 1.5 .5 3 -2 5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 20l4 -9l4 9"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.1 18h-6.2"
  }));
}

export { LanguageKatakana as default };
