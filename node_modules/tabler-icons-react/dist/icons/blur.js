import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Blur(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-blur",
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
    d: "M12 21a9.01 9.01 0 0 0 2.32 -.302a9.004 9.004 0 0 0 1.74 -16.733a9 9 0 1 0 -4.06 17.035z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 3v17"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 12h9"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 9h8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 6h6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 18h6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 15h8"
  }));
}

export { Blur as default };
